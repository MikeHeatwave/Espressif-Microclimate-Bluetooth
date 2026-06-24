package com.example.espressifmicroclimate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.espressifmicroclimate.data.model.BluetoothCommand
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState
import com.example.espressifmicroclimate.ui.components.ConnectionStatusBar
import com.example.espressifmicroclimate.ui.screens.AutoModeScreen
import com.example.espressifmicroclimate.ui.screens.MetricsScreen
import com.example.espressifmicroclimate.ui.screens.PresetsScreen
import com.example.espressifmicroclimate.ui.screens.RelaysScreen
import com.example.espressifmicroclimate.ui.screens.SettingsScreen
import com.example.espressifmicroclimate.ui.theme.EspressifMicroclimateTheme
import com.example.espressifmicroclimate.ui.theme.ThemeMode
import com.example.espressifmicroclimate.utils.loadSavedEspMacAddress
import com.example.espressifmicroclimate.utils.saveEspMacAddress
import com.example.espressifmicroclimate.utils.saveThemeMode
import kotlinx.coroutines.launch

// Разделы приложения, доступные в боковом меню
private enum class AppSection(val titleRes: Int) {
    Metrics(R.string.menu_metrics),
    Relays(R.string.menu_relays),
    AutoMode(R.string.menu_auto_mode),
    Presets(R.string.menu_presets),
    Settings(R.string.menu_settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EspressifMicroclimateTheme {
                MicroclimateApp()
            }
        }
    }
}

// Основной интерфейс приложения с ModalNavigationDrawer и Scaffold. Управляет темой, текущим разделом и автоподключением к ESP32 по Bluetooth
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MicroclimateApp() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Репозиторий Bluetooth - единый для всего приложения (singleton в Application)
    val application = context.applicationContext as MicroclimateApplication
    val bluetoothRepository = application.bluetoothRepository
    val connectionState by bluetoothRepository.connectionState.collectAsState()

    // Восстановление сохранённой темы и MAC-адреса при запуске
    var themeMode by remember { mutableStateOf(com.example.espressifmicroclimate.utils.loadThemeMode(context)) }
    var selectedSection by remember { mutableStateOf(AppSection.Metrics) }
    val savedMacAddress = remember { loadSavedEspMacAddress(context) }

    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Попытка подключиться к ESP32: сначала по сохранённому MAC-адресу, затем поиском устройства с именем ESP32_Microclimate среди сопряжённых
    // При успехе сохраняет MAC-адрес для будущих запусков
    val attemptAutoConnect = remember {
        suspend {
            when {
                !bluetoothRepository.isBluetoothSupported() -> {
                    Log.d("MicroclimateApp", "Bluetooth не поддерживается на этом устройстве")
                }
                !bluetoothRepository.isBluetoothEnabled() -> {
                    Log.d("MicroclimateApp", "Bluetooth выключен")
                }
                else -> {
                    val result = bluetoothRepository.autoConnect(savedMacAddress)
                    result.onSuccess { macAddress ->
                        saveEspMacAddress(context, macAddress)
                        Log.d("MicroclimateApp", "Автоподключение успешно: $macAddress")

                        // Отправляем текущее время на ESP32 для корректной работы расписания освещения
                        val unixTime = System.currentTimeMillis() / 1000 + 6 * 3600
                        bluetoothRepository.sendCommand(
                            BluetoothCommand.SetTime(unixTime)
                        )
                    }.onFailure { error ->
                        Log.d("MicroclimateApp", "Автоподключение не удалось: ${error.message}")
                    }
                }
            }
        }
    }

    // Лаунчер для запроса runtime-разрешений Bluetooth
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Разрешения получены - запускаем автоподключение
            scope.launch { attemptAutoConnect() }
        }
    }

    // При первом запуске запрашиваем разрешения и пытаемся подключиться
    LaunchedEffect(Unit) {
        val requiredPermissions = getRequiredBluetoothPermissions()
        val missingPermissions = requiredPermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            attemptAutoConnect()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // Отслеживаем включение/выключение Bluetooth в системе
    DisposableEffect(Unit) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    ?: return
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d("MicroclimateApp", "Bluetooth выключен в системе")
                        bluetoothRepository.disconnect()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.d("MicroclimateApp", "Bluetooth включён в системе")
                        scope.launch { attemptAutoConnect() }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    EspressifMicroclimateTheme(themeMode = themeMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Блокируем жест открытия меню, когда открыта клавиатура, чтобы не мешать вводу
            gesturesEnabled = !isKeyboardVisible,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.secondary,
                    drawerContentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = stringResource(R.string.drawer_menu),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 26.sp
                    )

                    // Пункты бокового меню для каждого раздела
                    AppSection.entries.forEach { section ->
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(section.titleRes)) },
                            selected = selectedSection == section,
                            onClick = {
                                selectedSection = section
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                            titleContentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                        ),
                        title = { Text(text = stringResource(R.string.project_title)) },
                        navigationIcon = {
                            IconButton(
                                enabled = !isKeyboardVisible,
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    scope.launch { drawerState.open() }
                                }
                            ) {
                                Image(
                                    painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                                    contentDescription = stringResource(R.string.drawer_menu)
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Панель подключения показывается на всех экранах, кроме настроек
                    if (selectedSection != AppSection.Settings) {
                        ConnectionStatusBar(
                            connectionState = connectionState,
                            onReconnect = {
                                scope.launch { attemptAutoConnect() }
                            }
                        )
                    }

                    SectionContent(
                        selectedSection = selectedSection,
                        themeMode = themeMode,
                        onThemeModeSelected = {
                            themeMode = it
                            saveThemeMode(context, it)
                        },
                        onNavigateToAutoMode = {
                            selectedSection = AppSection.AutoMode
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Возвращает список runtime-разрешений, необходимых для работы Bluetooth на текущей версии Android
private fun getRequiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

// Маршрутизатор, отображающий нужный экран в зависимости от выбранного раздела меню
@Composable
private fun SectionContent(
    selectedSection: AppSection,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNavigateToAutoMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedSection) {
        AppSection.Metrics -> MetricsScreen(modifier = modifier)
        AppSection.Relays -> RelaysScreen(modifier = modifier)
        AppSection.AutoMode -> AutoModeScreen(modifier = modifier)
        AppSection.Presets -> PresetsScreen(
            onNavigateToAutoMode = onNavigateToAutoMode,
            modifier = modifier
        )
        AppSection.Settings -> SettingsScreen(
            themeMode = themeMode,
            onThemeModeSelected = onThemeModeSelected,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MicroclimateAppPreview() {
    EspressifMicroclimateTheme {
        MicroclimateApp()
    }
}
