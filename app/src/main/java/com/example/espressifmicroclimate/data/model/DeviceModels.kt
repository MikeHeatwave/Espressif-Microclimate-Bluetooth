package com.example.espressifmicroclimate.data.model

import androidx.annotation.StringRes

// Данные для карточки показателя на главном экране: строковый ресурс заголовка и уже отформатированное значение
data class MetricCardData(
    @get:StringRes val titleRes: Int,
    val value: String
)
