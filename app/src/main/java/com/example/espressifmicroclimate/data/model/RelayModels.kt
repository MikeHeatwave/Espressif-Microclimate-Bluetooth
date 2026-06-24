package com.example.espressifmicroclimate.data.model

import androidx.annotation.StringRes
import com.example.espressifmicroclimate.R

/**
 * Модель реле для отображения в UI
 * key - технический ключ реле (fan, pump, lamp)
 * titleRes - строковый ресурс с человекочитаемым названием
 * modeAuto - включён ли автоматический режим
 * state - текущее состояние реле (вкл/выкл)
 */
data class RelayUiItem(
    val key: String,
    @get:StringRes val titleRes: Int,
    val modeAuto: Boolean,
    val state: Boolean
)

// Возвращает строковый ресурс названия реле по его техническому ключу
fun relayTitleResByKey(key: String): Int {
    return when (key) {
        "fan" -> R.string.relay_fan
        "pump" -> R.string.relay_pump
        "lamp" -> R.string.relay_lamp
        else -> R.string.relay_fan
    }
}
