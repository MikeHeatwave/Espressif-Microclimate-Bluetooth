package com.example.espressifmicroclimate.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.espressifmicroclimate.data.model.AutoModePreset
import com.example.espressifmicroclimate.utils.savePendingPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI-состояние экрана предустановок
 * navigateToAutoMode устанавливается в true после выбора культуры,
 * чтобы экран инициировал переход на экран "Параметры Auto Mode"
 */
data class PresetsUiState(
    val navigateToAutoMode: Boolean = false
)

/**
 * ViewModel для экрана готовых предустановок Auto Mode
 * При выборе культуры сохраняет её идентификатор в SharedPreferences
 * и сигнализирует UI о необходимости перехода на экран "Параметры Auto Mode"
 */
class PresetsViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetsUiState())
    val uiState: StateFlow<PresetsUiState> = _uiState.asStateFlow()

    // Сохраняет выбранную предустановку как "ожидающую применения" и запрашивает навигацию на экран Auto Mode
    fun applyPreset(preset: AutoModePreset) {
        savePendingPreset(context, preset)
        _uiState.update { it.copy(navigateToAutoMode = true) }
    }

    // Сбрасывает флаг навигации после того, как переход выполнен
    fun onNavigationHandled() {
        _uiState.update { it.copy(navigateToAutoMode = false) }
    }

    companion object {
        // Создаёт фабрику для передачи контекста в ViewModel
        fun createFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PresetsViewModel::class.java)) {
                        return PresetsViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
