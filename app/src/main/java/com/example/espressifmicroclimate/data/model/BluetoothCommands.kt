package com.example.espressifmicroclimate.data.model

// Команды, которые Android-приложение отправляет ESP32 по Bluetooth в формате JSON
// Каждая команда сериализуется в JSON-объект с полем "cmd", определяющим тип операции
sealed class BluetoothCommand {

    // Абстрактный тип команды, используется как значение поля "cmd" в JSON
    abstract val cmd: String

    // Запрос текущих показаний датчиков
    // ESP32 должен ответить JSON вида {"t":24.5,"h":58.0,"s":67.0,"l":420}
    data object RequestSensors : BluetoothCommand() {
        override val cmd = "getSensors"
    }

    // Запрос текущих состояний реле
    // ESP32 должен ответить JSON вида {"relays":{"fan":{"auto":false,"state":true},"pump":...}}
    data object GetRelays : BluetoothCommand() {
        override val cmd = "getRelays"
    }

    // Запрос текущих настроек Auto Mode
    // ESP32 должен ответить JSON вида {"auto":{"tMin":20,"tMax":25,"sMin":70,"lMin":50,"lMax":250,"lightMode":false,"lightStart":"20:00","lightEnd":"08:00"}}
    data object GetAutoMode : BluetoothCommand() {
        override val cmd = "getAutoMode"
    }

    // Установка текущего времени на ESP32
    // Передаётся Unix timestamp в секундах необходимый для работы режима освещения по расписанию
    data class SetTime(val unixTime: Long) : BluetoothCommand() {
        override val cmd = "setTime"
    }

    // Установка состояния одного реле
    data class SetRelay(
        val relay: String,
        val state: Boolean,
        val auto: Boolean
    ) : BluetoothCommand() {
        override val cmd = "setRelay"
    }

    // Установка параметров (настроек) Auto Mode
    data class SetAutoModeSettings(
        val temperatureMin: Int,
        val temperatureMax: Int,
        val soilMoistureMin: Int,
        val lightMin: Int,
        val lightMax: Int,
        val lightingScheduleMode: Boolean,
        val lightStartHour: Int,
        val lightStartMinute: Int,
        val lightEndHour: Int,
        val lightEndMinute: Int
    ) : BluetoothCommand() {
        override val cmd = "setAutoModeSettings"
    }
}
