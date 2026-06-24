package com.example.espressifmicroclimate.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.espressifmicroclimate.R

// Готовая предустановка Auto Mode под конкретную культуру
// Содержит название, изображение и рекомендуемые пороговые значения
data class AutoModePreset(
    val id: String,
    @get:StringRes val nameRes: Int,
    @get:DrawableRes val imageRes: Int,
    val temperatureMin: Int,
    val temperatureMax: Int,
    val soilMoistureMin: Int
)

// Список предустановок, доступных в разделе "Предустановки Auto Mode"
// Значения подобраны из открытых источников по агротехнике
val autoModePresets = listOf(
    AutoModePreset(
        id = "basil",
        nameRes = R.string.preset_basil,
        imageRes = R.drawable.basil,
        temperatureMin = 20,
        temperatureMax = 25,
        soilMoistureMin = 70
    ),
    AutoModePreset(
        id = "arugula",
        nameRes = R.string.preset_arugula,
        imageRes = R.drawable.arugula,
        temperatureMin = 18,
        temperatureMax = 22,
        soilMoistureMin = 65
    ),
    AutoModePreset(
        id = "lettuce",
        nameRes = R.string.preset_lettuce,
        imageRes = R.drawable.salad,
        temperatureMin = 15,
        temperatureMax = 20,
        soilMoistureMin = 75
    )
)
