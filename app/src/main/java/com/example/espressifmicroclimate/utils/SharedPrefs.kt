package com.example.espressifmicroclimate.utils

import android.content.Context
import androidx.core.content.edit
import com.example.espressifmicroclimate.data.model.AutoModeEditorState
import com.example.espressifmicroclimate.data.model.AutoModePreset
import com.example.espressifmicroclimate.data.model.ParsedAutoModeSettings
import com.example.espressifmicroclimate.data.model.RelayUiItem
import com.example.espressifmicroclimate.data.model.autoModePresets
import com.example.espressifmicroclimate.data.model.relayTitleResByKey
import com.example.espressifmicroclimate.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

private const val APP_PREFERENCES = "microclimate_preferences"
private const val THEME_MODE_KEY = "theme_mode"
private const val ESP_MAC_ADDRESS_KEY = "esp_mac_address"
private const val AUTO_MODE_SETTINGS_KEY = "auto_mode_settings"
private const val RELAY_STATES_KEY = "relay_states"
private const val PENDING_PRESET_ID_KEY = "pending_preset_id"

// Восстанавливает сохранённый режим темы, по умолчанию ThemeMode.SYSTEM
fun loadThemeMode(context: Context): ThemeMode {
    val saved = context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name)

    return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
}

// Сохраняет выбранный режим темы
fun saveThemeMode(context: Context, themeMode: ThemeMode) {
    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(THEME_MODE_KEY, themeMode.name)
        }
}

// Читает сохранённый MAC-адрес ESP32
// Используется для автоматического переподключения к известному устройству при запуске приложения
fun loadSavedEspMacAddress(context: Context): String? {
    return context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString(ESP_MAC_ADDRESS_KEY, null)
}

// Сохраняет MAC-адрес ESP32
// После первого успешного сопряжения/подключения адрес запоминается и используется для быстрого автоподключения в будущем
fun saveEspMacAddress(context: Context, macAddress: String) {
    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(ESP_MAC_ADDRESS_KEY, macAddress)
        }
}

// Сохраняет применённые параметры Auto Mode
// Используется для восстановления последних настроек при повторном открытии приложения
fun saveAutoModeSettings(context: Context, settings: ParsedAutoModeSettings) {
    val json = JSONObject().apply {
        put("temperatureMin", settings.temperatureMin)
        put("temperatureMax", settings.temperatureMax)
        put("soilMoistureMin", settings.soilMoistureMin)
        put("lightMin", settings.lightMin)
        put("lightMax", settings.lightMax)
        put("lightingScheduleMode", settings.lightingScheduleMode)
        put("lightStartHour", settings.lightStartHour)
        put("lightStartMinute", settings.lightStartMinute)
        put("lightEndHour", settings.lightEndHour)
        put("lightEndMinute", settings.lightEndMinute)
    }

    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(AUTO_MODE_SETTINGS_KEY, json.toString())
        }
}

// Восстанавливает сохранённые параметры Auto Mode
// Возвращает null, если настройки ещё не сохранялись
fun loadAutoModeSettings(context: Context): ParsedAutoModeSettings? {
    val jsonString = context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString(AUTO_MODE_SETTINGS_KEY, null)
        ?: return null

    return try {
        val json = JSONObject(jsonString)
        ParsedAutoModeSettings(
            temperatureMin = json.getInt("temperatureMin"),
            temperatureMax = json.getInt("temperatureMax"),
            soilMoistureMin = json.getInt("soilMoistureMin"),
            lightMin = json.getInt("lightMin"),
            lightMax = json.getInt("lightMax"),
            lightingScheduleMode = json.getBoolean("lightingScheduleMode"),
            lightStartHour = json.getInt("lightStartHour"),
            lightStartMinute = json.getInt("lightStartMinute"),
            lightEndHour = json.getInt("lightEndHour"),
            lightEndMinute = json.getInt("lightEndMinute")
        )
    } catch (_: Exception) {
        null
    }
}

// Преобразует распарсенные настройки Auto Mode в состояние редактора
// Используется для восстановления сохранённых значений в UI
fun ParsedAutoModeSettings.toEditorState(): AutoModeEditorState {
    return AutoModeEditorState(
        temperatureMin = temperatureMin.toString(),
        temperatureMax = temperatureMax.toString(),
        soilMoistureMin = soilMoistureMin.toString(),
        lightMin = lightMin.toString(),
        lightMax = lightMax.toString(),
        lightingScheduleMode = lightingScheduleMode,
        lightStartHour = lightStartHour,
        lightStartMinute = lightStartMinute,
        lightEndHour = lightEndHour,
        lightEndMinute = lightEndMinute
    )
}

// Сохраняет состояния реле
// Используется для восстановления последних настроек реле при повторном открытии приложения
fun saveRelayStates(context: Context, relays: List<RelayUiItem>) {
    val jsonArray = JSONArray()
    relays.forEach { relay ->
        val json = JSONObject().apply {
            put("key", relay.key)
            put("modeAuto", relay.modeAuto)
            put("state", relay.state)
        }
        jsonArray.put(json)
    }

    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(RELAY_STATES_KEY, jsonArray.toString())
        }
}

// Восстанавливает сохранённые состояния реле
// Возвращает null, если реле ещё не сохранялись
fun loadRelayStates(context: Context): List<RelayUiItem>? {
    val jsonString = context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString(RELAY_STATES_KEY, null)
        ?: return null

    return try {
        val jsonArray = JSONArray(jsonString)
        List(jsonArray.length()) { index ->
            val json = jsonArray.getJSONObject(index)
            RelayUiItem(
                key = json.getString("key"),
                titleRes = relayTitleResByKey(json.getString("key")),
                modeAuto = json.getBoolean("modeAuto"),
                state = json.getBoolean("state")
            )
        }
    } catch (_: Exception) {
        null
    }
}

// Сохраняет идентификатор выбранной предустановки
// Используется как мост между экраном предустановок и экраном Auto Mode
// При переходе Auto Mode считывает этот id и применяет параметры к полям редактора
fun savePendingPreset(context: Context, preset: AutoModePreset) {
    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(PENDING_PRESET_ID_KEY, preset.id)
        }
}

// Восстанавливает выбранную предустановку по сохранённому идентификатору
// Возвращает null, если предустановка не была выбрана или не найдена
fun loadPendingPreset(context: Context): AutoModePreset? {
    val id = context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString(PENDING_PRESET_ID_KEY, null)
        ?: return null

    return autoModePresets.firstOrNull { it.id == id }
}

// Очищает сохранённую предустановку после её применения
fun clearPendingPreset(context: Context) {
    context
        .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            remove(PENDING_PRESET_ID_KEY)
        }
}
