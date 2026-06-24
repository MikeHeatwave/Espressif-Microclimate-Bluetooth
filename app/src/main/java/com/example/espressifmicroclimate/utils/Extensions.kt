package com.example.espressifmicroclimate.utils

import java.util.Locale

// Форматирует целое число как двузначную строку (например, 5 - "05")
fun Int.twoDigits(): String {
    return String.format(Locale.US, "%02d", this)
}
