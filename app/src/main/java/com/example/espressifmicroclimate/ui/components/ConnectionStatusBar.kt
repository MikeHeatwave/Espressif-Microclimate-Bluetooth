package com.example.espressifmicroclimate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState

/**
 * Панель состояния подключения к ESP32 по Bluetooth
 * Отображается в верхней части экрана и показывает:
 * - идёт ли подключение;
 * - установлено ли соединение;
 * - произошла ли ошибка;
 * - отсутствует ли Bluetooth или разрешение
 *
 * Для состояний BluetoothConnectionState.Disconnected и BluetoothConnectionState.Error доступна кнопка повторного подключения
 */
@Composable
fun ConnectionStatusBar(
    connectionState: BluetoothConnectionState,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, statusText) = when (connectionState) {
        is BluetoothConnectionState.Connected -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            stringResource(R.string.connection_status_connected, connectionState.deviceName)
        )
        BluetoothConnectionState.Connecting -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            stringResource(R.string.connection_status_connecting)
        )
        BluetoothConnectionState.Disconnected -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.connection_status_disconnected)
        )
        is BluetoothConnectionState.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.connection_status_error, connectionState.message)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.connection_status_title),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (connectionState is BluetoothConnectionState.Disconnected ||
            connectionState is BluetoothConnectionState.Error
        ) {
            TextButton(
                onClick = onReconnect,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = contentColor
                )
            ) {
                Text(text = stringResource(R.string.connection_action_reconnect))
            }
        }
    }
}
