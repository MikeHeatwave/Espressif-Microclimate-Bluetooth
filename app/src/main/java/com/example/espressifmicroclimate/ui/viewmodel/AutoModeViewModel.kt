package com.example.espressifmicroclimate.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressifmicroclimate.data.model.AutoModeEditorState
import com.example.espressifmicroclimate.data.model.AutoModeValidation
import com.example.espressifmicroclimate.data.model.BluetoothCommand
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState
import com.example.espressifmicroclimate.data.model.LoadState
import com.example.espressifmicroclimate.data.repository.BluetoothRepository
import com.example.espressifmicroclimate.utils.clearPendingPreset
import com.example.espressifmicroclimate.utils.loadAutoModeSettings
import com.example.espressifmicroclimate.utils.loadPendingPreset
import com.example.espressifmicroclimate.utils.saveAutoModeSettings
import com.example.espressifmicroclimate.utils.toEditorState
import com.example.espressifmicroclimate.utils.toParsedAutoMode
import com.example.espressifmicroclimate.utils.validateAutoMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-состояние экрана Auto Mode
 * editorState - текущие значения полей ввода
 * originalSettings - значения, синхронизированные с устройством
 * hasPendingChanges показывает, отличается ли редактор от сохранённого состояния
 */
data class AutoModeUiState(
    val editorState: AutoModeEditorState = AutoModeEditorState(),
    val originalSettings: AutoModeEditorState = AutoModeEditorState(),
    val loadState: LoadState = LoadState.Idle,
    val errorMessage: String? = null
) {
    val validation: AutoModeValidation
        get() = validateAutoMode(editorState)

    val hasPendingChanges: Boolean
        get() = editorState != originalSettings
}

/**
 * ViewModel для редактирования пороговых значений и расписания Auto Mode
 * При инициализации восстанавливает последние сохранённые настройки из SharedPreferences,
 * применяет выбранную предустановку (если пользователь перешёл с экрана предустановок)
 * и подписывается на поток актуальных настроек с ESP32
 * При сохранении отправляет JSON-команду BluetoothCommand.SetAutoModeSettings на ESP32 и сохраняет параметры в SharedPreferences
 */
class AutoModeViewModel(
    private val repository: BluetoothRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoModeUiState())
    val uiState: StateFlow<AutoModeUiState> = _uiState.asStateFlow()

    init {
        val pendingPreset = loadPendingPreset(context)
        val initialState = buildInitialEditorState(pendingPreset)
        clearPendingPreset(context)
        _uiState.value = AutoModeUiState(
            editorState = initialState,
            originalSettings = initialState,
            loadState = LoadState.Idle
        )

        // Подписка на актуальные настройки Auto Mode с ESP32
        viewModelScope.launch {
            repository.autoModeSettings.collect { settings ->
                settings?.let { settingsFromEsp ->
                    // Сохраняем полученные настройки только если пользователь не вносил локальных изменений, которые ещё не отправлены
                    if (_uiState.value.editorState == _uiState.value.originalSettings) {
                        saveAutoModeSettings(context, settingsFromEsp)
                        val editorState = settingsFromEsp.toEditorState()
                        _uiState.update {
                            it.copy(
                                editorState = editorState,
                                originalSettings = editorState,
                                loadState = LoadState.Idle
                            )
                        }
                    }
                }
            }
        }

        // Запрашиваем текущие настройки Auto Mode, если уже есть подключение и не была выбрана предустановка (в этом случае приоритет отдаём ей)
        if (pendingPreset == null) {
            viewModelScope.launch {
                if (repository.connectionState.value is BluetoothConnectionState.Connected) {
                    repository.requestAutoMode()
                }
            }
        }
    }

    /**
     * Формирует начальное состояние редактора:
     * Если есть выбранная предустановка (переход с экрана предустановок) - применяет её
     * Иначе загружает последние сохранённые настройки Auto Mode
     * Если ничего не сохранено - использует пустой редактор
     */
    private fun buildInitialEditorState(pendingPreset: com.example.espressifmicroclimate.data.model.AutoModePreset?): AutoModeEditorState {
        val savedSettings = loadAutoModeSettings(context)

        val presetEditorState = pendingPreset?.let {
            AutoModeEditorState(
                temperatureMin = it.temperatureMin.toString(),
                temperatureMax = it.temperatureMax.toString(),
                soilMoistureMin = it.soilMoistureMin.toString()
            )
        }

        val savedEditorState = savedSettings?.toEditorState()

        return when {
            presetEditorState != null && savedEditorState != null -> {
                // Параметры температуры и влажности берём из пресета, параметры освещения - из ранее сохранённых настроек
                savedEditorState.copy(
                    temperatureMin = presetEditorState.temperatureMin,
                    temperatureMax = presetEditorState.temperatureMax,
                    soilMoistureMin = presetEditorState.soilMoistureMin
                )
            }
            presetEditorState != null -> presetEditorState
            savedEditorState != null -> savedEditorState
            else -> AutoModeEditorState()
        }
    }

    // Проверяет, была ли выбрана предустановка на экране предустановок и применяет её параметры к редактору
    // Вызывается из экрана при каждом входе, так как ViewModel может оставаться живым между переключениями разделов
    fun applyPendingPresetIfNeeded() {
        val pendingPreset = loadPendingPreset(context) ?: return
        clearPendingPreset(context)

        val savedSettings = loadAutoModeSettings(context)
        val presetEditorState = AutoModeEditorState(
            temperatureMin = pendingPreset.temperatureMin.toString(),
            temperatureMax = pendingPreset.temperatureMax.toString(),
            soilMoistureMin = pendingPreset.soilMoistureMin.toString()
        )

        val newState = when {
            savedSettings != null -> savedSettings.toEditorState().copy(
                temperatureMin = presetEditorState.temperatureMin,
                temperatureMax = presetEditorState.temperatureMax,
                soilMoistureMin = presetEditorState.soilMoistureMin
            )
            else -> presetEditorState
        }

        _uiState.update { it.copy(editorState = newState) }
    }

    /**
     * Валидирует, сохраняет локально и отправляет настройки Auto Mode на ESP32
     * Сначала параметры записываются в SharedPreferences, чтобы пользователь видел последние выбранные настройки даже без активного Bluetooth-подключения
     * Затем выполняется отправка команды BluetoothCommand.SetAutoModeSettings по Bluetooth
     * originalSettings обновляется только после успешной отправки, чтобы кнопка "Применить параметры Auto Mode" оставалась активной при неудаче
     */
    fun save(fixFieldsMessage: String) {
        val current = _uiState.value.editorState
        val validation = validateAutoMode(current)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(loadState = LoadState.Error, errorMessage = fixFieldsMessage)
            }
            return
        }

        val parsed = current.toParsedAutoMode()
        if (parsed == null) {
            _uiState.update {
                it.copy(loadState = LoadState.Error, errorMessage = fixFieldsMessage)
            }
            return
        }

        _uiState.update { it.copy(loadState = LoadState.Loading, errorMessage = null) }

        // Сохраняем настройки локально независимо от результата отправки
        saveAutoModeSettings(context, parsed)

        viewModelScope.launch {
            val command = BluetoothCommand.SetAutoModeSettings(
                temperatureMin = parsed.temperatureMin,
                temperatureMax = parsed.temperatureMax,
                soilMoistureMin = parsed.soilMoistureMin,
                lightMin = parsed.lightMin,
                lightMax = parsed.lightMax,
                lightingScheduleMode = parsed.lightingScheduleMode,
                lightStartHour = parsed.lightStartHour,
                lightStartMinute = parsed.lightStartMinute,
                lightEndHour = parsed.lightEndHour,
                lightEndMinute = parsed.lightEndMinute
            )

            val result = repository.sendCommand(command)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(originalSettings = current, loadState = LoadState.Idle)
                }
            } else {
                _uiState.update {
                    it.copy(
                        loadState = LoadState.Error,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    // Обновляет состояние редактора через лямбду, используется для полей ввода и переключателей
    fun updateEditor(update: (AutoModeEditorState) -> AutoModeEditorState) {
        _uiState.update { it.copy(editorState = update(it.editorState)) }
    }

    companion object {
        // Создаёт фабрику для передачи BluetoothRepository и контекста в ViewModel
        fun createFactory(
            repository: BluetoothRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AutoModeViewModel::class.java)) {
                    return AutoModeViewModel(repository, context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
