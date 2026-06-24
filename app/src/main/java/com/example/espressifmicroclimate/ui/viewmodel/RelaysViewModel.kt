package com.example.espressifmicroclimate.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.espressifmicroclimate.data.model.BluetoothCommand
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState
import com.example.espressifmicroclimate.data.model.LoadState
import com.example.espressifmicroclimate.data.model.RelayUiItem
import com.example.espressifmicroclimate.data.repository.BluetoothRepository
import com.example.espressifmicroclimate.utils.loadRelayStates
import com.example.espressifmicroclimate.utils.relayDefinitions
import com.example.espressifmicroclimate.utils.saveRelayStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-состояние экрана реле
 * relays - актуальное состояние переключателей в UI
 * originalRelays - состояние, синхронизированное с устройством
 * hasPendingChanges показывает, есть ли несохранённые изменения
 */
data class RelaysUiState(
    val relays: List<RelayUiItem> = emptyList(),
    val originalRelays: List<RelayUiItem> = emptyList(),
    val loadState: LoadState = LoadState.Idle,
    val errorMessage: String? = null
) {
    val hasPendingChanges: Boolean get() = relays != originalRelays
}

/**
 * ViewModel для управления реле (ручной/авто режим и состояние)
 * При инициализации восстанавливает последние сохранённые состояния реле из SharedPreferences
 * и подписывается на поток актуальных состояний с ESP32
 * Кнопка "Изменить реле" отправляет на ESP32 JSON-команды BluetoothCommand.SetRelay
 * для каждого реле: передаётся режим (auto/manual) и состояние (для manual)
 * При успешной отправке originalRelays обновляется, при неудаче кнопка остаётся активной
 */
class RelaysViewModel(
    private val repository: BluetoothRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelaysUiState())
    val uiState: StateFlow<RelaysUiState> = _uiState.asStateFlow()

    init {
        refresh()

        // Подписка на актуальные состояния реле с ESP32
        viewModelScope.launch {
            repository.relayStates.collect { newRelays ->
                newRelays?.let { relaysFromEsp ->
                    // Сохраняем полученные состояния только если пользователь не вносил локальных изменений, которые ещё не отправлены
                    if (_uiState.value.relays == _uiState.value.originalRelays) {
                        saveRelayStates(context, relaysFromEsp)
                        _uiState.update {
                            it.copy(
                                relays = relaysFromEsp,
                                originalRelays = relaysFromEsp,
                                loadState = LoadState.Idle
                            )
                        }
                    }
                }
            }
        }

        // Запрашиваем текущие состояния реле если уже есть подключение
        viewModelScope.launch {
            if (repository.connectionState.value is BluetoothConnectionState.Connected) {
                repository.requestRelays()
            }
        }
    }

    // Загружает текущие состояния и режимы реле
    fun refresh() {
        _uiState.update { it.copy(loadState = LoadState.Loading, errorMessage = null) }

        val savedRelays = loadRelayStates(context)
        val defaultRelays = relayDefinitions()

        // Если сохранённые состояния содержат все известные реле - используем их, иначе используем значения по умолчанию
        val relays = if (savedRelays != null && savedRelays.size == defaultRelays.size) {
            savedRelays
        } else {
            defaultRelays
        }

        _uiState.update {
            it.copy(
                relays = relays,
                originalRelays = relays,
                loadState = LoadState.Idle
            )
        }
    }

    /**
     * Сохраняет текущие настройки реле в SharedPreferences и отправляет их на ESP32
     * Сначала состояния записываются в память смартфона, чтобы пользователь видел
     * последние выбранные настройки даже без активного Bluetooth-подключения
     * Затем выполняется отправка команд по Bluetooth
     * originalRelays обновляется только после успешной отправки,
     * чтобы кнопка "Изменить реле" оставалась активной при неудаче
     */
    fun save() {
        val currentRelays = _uiState.value.relays
        _uiState.update { it.copy(loadState = LoadState.Loading, errorMessage = null) }

        // Сохраняем выбранные состояния локально независимо от результата отправки
        saveRelayStates(context, currentRelays)

        viewModelScope.launch {
            val results = currentRelays.map { relay ->
                repository.sendCommand(
                    BluetoothCommand.SetRelay(
                        relay = relay.key,
                        state = relay.state,
                        auto = relay.modeAuto
                    )
                )
            }

            val firstFailure = results.firstOrNull { it.isFailure }
            if (firstFailure != null) {
                _uiState.update {
                    it.copy(
                        loadState = LoadState.Error,
                        errorMessage = firstFailure.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        originalRelays = currentRelays,
                        loadState = LoadState.Idle
                    )
                }
            }
        }
    }

    // Переключает режим работы реле (автоматический/ручной)
    fun updateRelayAuto(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                relays = state.relays.map { relay ->
                    if (relay.key == key) relay.copy(modeAuto = value) else relay
                }
            )
        }
    }

    // Переключает физическое состояние реле (вкл/выкл)
    fun updateRelayState(key: String, value: Boolean) {
        _uiState.update { state ->
            state.copy(
                relays = state.relays.map { relay ->
                    if (relay.key == key) relay.copy(state = value) else relay
                }
            )
        }
    }

    companion object {
        // Создаёт фабрику для передачи BluetoothRepository и контекста в ViewModel
        fun createFactory(
            repository: BluetoothRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RelaysViewModel::class.java)) {
                    return RelaysViewModel(repository, context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
