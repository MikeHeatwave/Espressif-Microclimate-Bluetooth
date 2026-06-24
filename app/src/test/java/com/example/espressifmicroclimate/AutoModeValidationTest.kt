package com.example.espressifmicroclimate
import com.example.espressifmicroclimate.data.model.AutoModeEditorState
import com.example.espressifmicroclimate.utils.toParsedAutoMode
import com.example.espressifmicroclimate.utils.validateAutoMode
import org.junit.Assert.*
import org.junit.Test

class AutoModeValidationTest {
    // Проверка корректных данных
    @Test
    fun validateAutoMode_validData_returnsNoErrors() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNull(result.temperatureMinError)
        assertNull(result.temperatureMaxError)
        assertNull(result.soilMoistureMinError)
        assertNull(result.lightMinError)
        assertNull(result.lightMaxError)
    }

    // Проверка неверного формата температуры
    @Test
    fun validateAutoMode_temperatureNotInteger_returnsError() {
        val state = AutoModeEditorState(
            temperatureMin = "22.5",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNotNull(result.temperatureMinError)
    }

    // Проверка диапазона влажности почвы
    @Test
    fun validateAutoMode_soilMoistureOutOfRange_returnsError() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "28",
            soilMoistureMin = "120",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNotNull(result.soilMoistureMinError)
    }

    // Проверка минимальная температура больше максимальной
    @Test
    fun validateAutoMode_temperatureMinGreaterThanMax_returnsError() {
        val state = AutoModeEditorState(
            temperatureMin = "30",
            temperatureMax = "25",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNotNull(result.temperatureMinError)
        assertNotNull(result.temperatureMaxError)
    }

    // Проверка разницы температур меньше 2 градусов
    @Test
    fun validateAutoMode_temperatureGapTooSmall_returnsError() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "23",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNotNull(result.temperatureMinError)
        assertNotNull(result.temperatureMaxError)
    }

    // Проверка диапазона освещенности
    @Test
    fun validateAutoMode_lightDifferenceTooSmall_returnsError() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "100",
            lightMax = "120",
            lightingScheduleMode = false
        )
        val result = validateAutoMode(state)
        assertNotNull(result.lightMinError)
        assertNotNull(result.lightMaxError)
    }

    // Проверка режима расписания освещения
    @Test
    fun validateAutoMode_scheduleMode_ignoresLightValues() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "",
            lightMax = "",
            lightingScheduleMode = true
        )
        val result = validateAutoMode(state)
        assertNull(result.lightMinError)
        assertNull(result.lightMaxError)
    }

    // Проверка преобразования String -> ParsedAutoModeSettings
    @Test
    fun toParsedAutoMode_validData_returnsParsedObject() {
        val state = AutoModeEditorState(
            temperatureMin = "22",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false,
            lightStartHour = 9,
            lightStartMinute = 0,
            lightEndHour = 21,
            lightEndMinute = 0
        )
        val result = state.toParsedAutoMode()
        assertNotNull(result)
        assertEquals(22, result!!.temperatureMin)
        assertEquals(28, result.temperatureMax)
        assertEquals(70, result.soilMoistureMin)
        assertEquals(40, result.lightMin)
        assertEquals(300, result.lightMax)
        assertEquals(9, result.lightStartHour)
        assertEquals(21, result.lightEndHour)
    }

    // Проверка ошибки преобразования
    @Test
    fun toParsedAutoMode_invalidTemperature_returnsNull() {
        val state = AutoModeEditorState(
            temperatureMin = "abc",
            temperatureMax = "28",
            soilMoistureMin = "70",
            lightMin = "40",
            lightMax = "300",
            lightingScheduleMode = false
        )
        val result = state.toParsedAutoMode()
        assertNull(result)
    }
}