package com.example.espressifmicroclimate.utils

import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.AutoModeEditorState
import com.example.espressifmicroclimate.data.model.AutoModeValidation
import com.example.espressifmicroclimate.data.model.ParsedAutoModeSettings

/**
 * Проверяет корректность полей редактора Auto Mode
 * Возвращает объект с ошибками для каждого поля (null - ошибки нет)
 * Правила валидации:
 * - температура и влажность почвы должны быть целыми числами;
 * - влажность почвы в диапазоне 0–100 %;
 * - минимальная температура не должна превышать максимальную, разница не менее 2 °C;
 * - для порогового режима освещения: lightMin ≤ lightMax, разница не менее 50
 */
fun validateAutoMode(state: AutoModeEditorState): AutoModeValidation {
    val tempMin = state.temperatureMin.toIntOrNull()
    val tempMax = state.temperatureMax.toIntOrNull()
    val soilMin = state.soilMoistureMin.toIntOrNull()
    val lightMin = state.lightMin.toIntOrNull()
    val lightMax = state.lightMax.toIntOrNull()

    var tempMinError: Int? = null
    var tempMaxError: Int? = null
    var soilError: Int? = null
    var lightMinError: Int? = null
    var lightMaxError: Int? = null

    if (tempMin == null) tempMinError = R.string.auto_mode_error_integer
    if (tempMax == null) tempMaxError = R.string.auto_mode_error_integer
    if (soilMin == null) {
        soilError = R.string.auto_mode_error_integer
    } else if (soilMin !in 0..100) {
        soilError = R.string.auto_mode_error_soil_range
    }

    if (tempMin != null && tempMax != null) {
        when {
            tempMin > tempMax -> {
                tempMinError = R.string.auto_mode_error_temp_order
                tempMaxError = R.string.auto_mode_error_temp_order
            }
            tempMax - tempMin < 2 -> {
                tempMinError = R.string.auto_mode_error_temp_gap
                tempMaxError = R.string.auto_mode_error_temp_gap
            }
        }
    }

    // В режиме расписания освещения пороги не используются - не валидируем
    if (!state.lightingScheduleMode) {
        if (lightMin == null) lightMinError = R.string.auto_mode_error_integer
        if (lightMax == null) lightMaxError = R.string.auto_mode_error_integer

        if (lightMin != null && lightMax != null) {
            when {
                lightMin > lightMax -> {
                    lightMinError = R.string.auto_mode_error_light_order
                    lightMaxError = R.string.auto_mode_error_light_order
                }
                lightMax - lightMin < 50 -> {
                    lightMinError = R.string.auto_mode_error_light_gap
                    lightMaxError = R.string.auto_mode_error_light_gap
                }
            }
        }
    }

    return AutoModeValidation(
        temperatureMinError = tempMinError,
        temperatureMaxError = tempMaxError,
        soilMoistureMinError = soilError,
        lightMinError = lightMinError,
        lightMaxError = lightMaxError
    )
}

// Преобразует строковое состояние редактора в числовые настройки
fun AutoModeEditorState.toParsedAutoMode(): ParsedAutoModeSettings? {
    val tempMin = temperatureMin.toIntOrNull() ?: return null
    val tempMax = temperatureMax.toIntOrNull() ?: return null
    val soilMin = soilMoistureMin.toIntOrNull() ?: return null
    val parsedLightMin = lightMin.toIntOrNull() ?: 0
    val parsedLightMax = lightMax.toIntOrNull() ?: 0

    return ParsedAutoModeSettings(
        temperatureMin = tempMin,
        temperatureMax = tempMax,
        soilMoistureMin = soilMin,
        lightMin = parsedLightMin,
        lightMax = parsedLightMax,
        lightingScheduleMode = lightingScheduleMode,
        lightStartHour = lightStartHour,
        lightStartMinute = lightStartMinute,
        lightEndHour = lightEndHour,
        lightEndMinute = lightEndMinute
    )
}
