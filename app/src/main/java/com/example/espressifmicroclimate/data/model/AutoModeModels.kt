package com.example.espressifmicroclimate.data.model

// Состояние редактора параметров Auto Mode
// Поля хранятся как строки, чтобы пользователь мог вводить значения постепенно
data class AutoModeEditorState(
    val temperatureMin: String = "",
    val temperatureMax: String = "",
    val soilMoistureMin: String = "",
    val lightMin: String = "",
    val lightMax: String = "",
    val lightingScheduleMode: Boolean = false,
    val lightStartHour: Int = 0,
    val lightStartMinute: Int = 0,
    val lightEndHour: Int = 0,
    val lightEndMinute: Int = 0
)

// Результат валидации полей Auto Mode
// Для каждого поля хранится либо null (нет ошибки), либо строковый ресурс с описанием ошибки
data class AutoModeValidation(
    val temperatureMinError: Int? = null,
    val temperatureMaxError: Int? = null,
    val soilMoistureMinError: Int? = null,
    val lightMinError: Int? = null,
    val lightMaxError: Int? = null
) {
    val isValid: Boolean
        get() = temperatureMinError == null &&
            temperatureMaxError == null &&
            soilMoistureMinError == null &&
            lightMinError == null &&
            lightMaxError == null
}

// Успешно распарсенные и валидные настройки Auto Mode
// Используется перед отправкой в ESP32: все значения гарантированно являются числами
data class ParsedAutoModeSettings(
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
)
