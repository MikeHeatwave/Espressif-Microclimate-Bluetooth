package com.example.espressifmicroclimate.data.model

// Состояние загрузки данных из Bluetooth
// Используется во всех экранах для отображения статуса операции
enum class LoadState {
    Idle,    // Начальное состояние, данные ещё не загружались
    Loading, // Идёт загрузка или отправка данных
    Success, // Операция завершилась успешно
    Error    // Произошла ошибка
}

// Текущие показатели датчиков с устройства
// Все значения nullable, так как при отсутствии связи данные могут быть недоступны
data class CurrentSensors(
    val temperature: Double? = null,
    val airHumidity: Double? = null,
    val soilMoisture: Double? = null,
    val light: Double? = null
)
