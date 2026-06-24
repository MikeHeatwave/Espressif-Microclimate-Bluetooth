package com.example.espressifmicroclimate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState
import com.example.espressifmicroclimate.data.model.CurrentSensors
import com.example.espressifmicroclimate.data.repository.BluetoothRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-состояние экрана "Показатели"
 * Содержит текущие значения датчиков
 */
data class MetricsUiState(
    val sensors: CurrentSensors = CurrentSensors()
)

/**
 * ViewModel для получения и отображения текущих показаний с датчиков
 * Подписывается на поток BluetoothRepository.currentSensors и автоматически
 * запрашивает обновление показаний у ESP32 каждые POLLING_INTERVAL_MS миллисекунд
 * если установлено активное Bluetooth-подключение
 */
class MetricsViewModel(private val repository: BluetoothRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    init {
        // Подписка на входящие показания датчиков
        viewModelScope.launch {
            repository.currentSensors.collect { sensors ->
                _uiState.update { it.copy(sensors = sensors ?: CurrentSensors()) }
            }
        }

        // Периодический опрос ESP32
        viewModelScope.launch {
            while (isActive) {
                if (repository.connectionState.value is BluetoothConnectionState.Connected) {
                    repository.requestSensors()
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    companion object {
        // Периодичность запроса показаний с ESP32 в миллисекундах
        private const val POLLING_INTERVAL_MS = 2000L

        // Создаёт фабрику для передачи BluetoothRepository в ViewModel
        // Используется в Compose через viewModel(factory = MetricsViewModel.createFactory(repository))
        fun createFactory(repository: BluetoothRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MetricsViewModel::class.java)) {
                        return MetricsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
