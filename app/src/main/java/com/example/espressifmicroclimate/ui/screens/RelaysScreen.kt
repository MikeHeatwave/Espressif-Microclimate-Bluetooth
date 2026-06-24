package com.example.espressifmicroclimate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressifmicroclimate.MicroclimateApplication
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.LoadState
import com.example.espressifmicroclimate.ui.components.RelayCard
import com.example.espressifmicroclimate.ui.viewmodel.RelaysViewModel

/**
 * Экран управления реле
 * Для каждого реле можно переключить режим (авто/ручной) и состояние (вкл/выкл)
 * Кнопка "Изменить реле" отправляет настройки на ESP32 по Bluetooth и сохраняет их в SharedPreferences
 */
@Composable
fun RelaysScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as MicroclimateApplication
    val viewModel: RelaysViewModel = viewModel(
        factory = RelaysViewModel.createFactory(
            repository = application.bluetoothRepository,
            context = context.applicationContext
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.relays_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.relays, key = { it.key }) { relay ->
                RelayCard(
                    relay = relay,
                    onAutoModeChanged = { value ->
                        viewModel.updateRelayAuto(relay.key, value)
                    },
                    onStateChanged = { value ->
                        viewModel.updateRelayState(relay.key, value)
                    }
                )
            }
        }

        // Кнопка сохранения доступна только если есть изменения и нет активной отправки
        Button(
            onClick = { viewModel.save() },
            enabled = uiState.loadState != LoadState.Loading &&
                uiState.relays.isNotEmpty() &&
                uiState.hasPendingChanges,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.save_relays))
        }
    }
}
