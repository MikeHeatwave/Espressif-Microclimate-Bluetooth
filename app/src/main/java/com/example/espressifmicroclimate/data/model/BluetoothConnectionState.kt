package com.example.espressifmicroclimate.data.model

// Состояние подключения к Bluetooth-устройству
sealed class BluetoothConnectionState {
    // Нет активного подключения
    data object Disconnected : BluetoothConnectionState()

    // Идёт процесс подключения
    data object Connecting : BluetoothConnectionState()

    // Подключение установлено, deviceName - имя подключённого устройства
    data class Connected(val deviceName: String) : BluetoothConnectionState()

    // Произошла ошибка, message содержит описание
    data class Error(val message: String) : BluetoothConnectionState()
}
