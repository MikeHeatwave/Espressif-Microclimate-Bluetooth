package com.example.espressifmicroclimate.utils

import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.RelayUiItem
import java.util.Locale

// Форматирует числовое значение датчика с единицами измерения
// Целые числа выводятся без дробной части, иначе - с одним знаком после запятой
fun formatMetricValue(value: Double?, unit: String, noDataText: String): String {
    if (value == null) {
        return noDataText
    }

    val formattedNumber = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

    return "$formattedNumber $unit"
}

// Список реле, известных приложению, с человекочитаемыми названиями
fun relayDefinitions(): List<RelayUiItem> {
    return listOf(
        RelayUiItem("fan", R.string.relay_fan, modeAuto = false, state = false),
        RelayUiItem("pump", R.string.relay_pump, modeAuto = false, state = false),
        RelayUiItem("lamp", R.string.relay_lamp, modeAuto = false, state = false)
    )
}
