package com.example.espressifmicroclimate

import android.app.Application
import com.example.espressifmicroclimate.data.repository.BluetoothRepository

// Класс, который хранит единственный экземпляр BluetoothRepository, который используется всеми ViewModel и UI-компонентами приложения
// Это гарантирует, что состояние подключения к ESP32 едино для всего приложения
class MicroclimateApplication : Application() {

    // Репозиторий Bluetooth, который создаётся при первом обращении
    val bluetoothRepository: BluetoothRepository by lazy {
        BluetoothRepository(applicationContext)
    }
}
