package com.example.espressifmicroclimate.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.espressifmicroclimate.data.model.BluetoothCommand
import com.example.espressifmicroclimate.data.model.BluetoothConnectionState
import com.example.espressifmicroclimate.data.model.CurrentSensors
import com.example.espressifmicroclimate.data.model.ParsedAutoModeSettings
import com.example.espressifmicroclimate.data.model.RelayUiItem
import com.example.espressifmicroclimate.data.model.relayTitleResByKey
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Репозиторий для прямого взаимодействия с ESP32 по Bluetooth Classic
 * Основные возможности:
 * - получение списка сопряжённых устройств;
 * - поиск ESP32 по имени ESP_DEVICE_NAME;
 * - автоподключение к сохранённому MAC-адресу или найденному устройству;
 * - отправка JSON-команд;
 * - приём и парсинг входящих JSON-данных через StateFlow: показания датчиков, состояния реле, настройки Auto Mode
 */
class BluetoothRepository(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private var readJob: Job? = null

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _currentSensors = MutableStateFlow<CurrentSensors?>(null)
    val currentSensors: StateFlow<CurrentSensors?> = _currentSensors.asStateFlow()

    private val _relayStates = MutableStateFlow<List<RelayUiItem>?>(null)
    val relayStates: StateFlow<List<RelayUiItem>?> = _relayStates.asStateFlow()

    private val _autoModeSettings = MutableStateFlow<ParsedAutoModeSettings?>(null)
    val autoModeSettings: StateFlow<ParsedAutoModeSettings?> = _autoModeSettings.asStateFlow()

    // Возвращает true если на устройстве есть Bluetooth-адаптер
    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    // Возвращает true если Bluetooth включён
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Автоматически подключается к ESP32
     * Если передан savedAddress - сначала пытаемся подключиться к нему
     * Если подключение по сохранённому адресу не удалось или адреса нет -
     * ищем устройство с именем ESP_DEVICE_NAME среди сопряжённых и подключаемся к нему
     */
    @Suppress("MissingPermission")
    suspend fun autoConnect(savedAddress: String?): Result<String> = withContext(Dispatchers.IO) {
        if (!hasConnectPermission()) {
            return@withContext Result.failure(
                SecurityException("Нет разрешения на подключение Bluetooth")
            )
        }

        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth адаптер недоступен"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth выключен"))
        }

        // Сначала пробуем сохранённый адрес если он есть
        if (!savedAddress.isNullOrBlank()) {
            try {
                connect(savedAddress)
                if (_connectionState.value is BluetoothConnectionState.Connected) {
                    return@withContext Result.success(savedAddress)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Не удалось подключиться к сохранённому адресу $savedAddress: ${e.message}")
            }
        }

        // Если сохранённый адрес не сработал - ищем устройство по имени
        val espDevice = adapter.bondedDevices.firstOrNull { it.name == ESP_DEVICE_NAME }
            ?: return@withContext Result.failure(
                IllegalStateException("Устройство $ESP_DEVICE_NAME не найдено среди сопряжённых")
            )

        return@withContext try {
            connect(espDevice.address)
            if (_connectionState.value is BluetoothConnectionState.Connected) {
                Result.success(espDevice.address)
            } else {
                Result.failure(IllegalStateException("Не удалось подключиться к $ESP_DEVICE_NAME"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Подключается к устройству по MAC-адресу
    // При успехе запускает фоновое чтение входящих данных и запрашивает текущие показания, состояния реле и настройки Auto Mode
    @Suppress("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasConnectPermission()) {
            return@withContext Result.failure(
                SecurityException("Нет разрешения на подключение Bluetooth")
            )
        }

        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth адаптер недоступен"))

        try {
            _connectionState.value = BluetoothConnectionState.Connecting
            disconnectInternal()

            val device = adapter.getRemoteDevice(address)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            bluetoothSocket = socket
            outputStream = socket.outputStream
            reader = BufferedReader(InputStreamReader(socket.inputStream))

            val deviceName = device.name ?: address
            _connectionState.value = BluetoothConnectionState.Connected(deviceName)
            startReading()

            // Сразу после подключения запрашиваем актуальные данные с ESP32
            requestSensors()
            requestRelays()
            requestAutoMode()

            Result.success(Unit)
        } catch (e: IOException) {
            disconnectInternal()
            _connectionState.value = BluetoothConnectionState.Error(
                e.message ?: "Не удалось подключиться к устройству"
            )
            Result.failure(e)
        }
    }

    // Отправляет строковую команду на подключённое устройство
    suspend fun sendCommand(command: String): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream
            ?: return@withContext Result.failure(IllegalStateException("Нет активного подключения"))

        return@withContext try {
            stream.write((command + "\n").toByteArray(Charsets.UTF_8))
            stream.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            _connectionState.value = BluetoothConnectionState.Error(
                e.message ?: "Ошибка отправки команды"
            )
            Result.failure(e)
        }
    }

    // Отправляет JSON-команду на ESP32
    // Автоматически сериализует BluetoothCommand в строку
    suspend fun sendCommand(command: BluetoothCommand): Result<Unit> {
        return sendCommand(command.toJson())
    }

    // Запрашивает текущие показания датчиков у ESP32
    suspend fun requestSensors(): Result<Unit> {
        return sendCommand(BluetoothCommand.RequestSensors)
    }

    // Запрашивает текущие состояния реле у ESP32
    suspend fun requestRelays(): Result<Unit> {
        return sendCommand(BluetoothCommand.GetRelays)
    }

    // Запрашивает текущие настройки Auto Mode у ESP32
    suspend fun requestAutoMode(): Result<Unit> {
        return sendCommand(BluetoothCommand.GetAutoMode)
    }

    // Разрывает соединение, сбрасывает полученные данные и освобождает ресурсы
    fun disconnect() {
        disconnectInternal()
        _currentSensors.value = null
        _relayStates.value = null
        _autoModeSettings.value = null
        _connectionState.value = BluetoothConnectionState.Disconnected
    }

    // Проверяет наличие разрешения на подключение к Bluetooth-устройствам
    private fun hasConnectPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Запускает фоновую корутину для чтения входящих строк
    private fun startReading() {
        readJob?.cancel()
        readJob = repositoryScope.launch {
            val currentReader = reader ?: return@launch
            while (isActive) {
                try {
                    val line = currentReader.readLine()
                    if (line != null) {
                        parseIncomingData(line)
                    } else {
                        // Поток закрыт - соединение разорвано
                        break
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        _connectionState.value = BluetoothConnectionState.Error(
                            e.message ?: "Ошибка приёма данных"
                        )
                    }
                    break
                }
            }
            // Если цикл чтения завершился, то считаем, что устройство отключено
            if (isActive) {
                disconnectInternal()
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }
    }

    // Распознаёт тип входящей JSON-строки и направляет её в соответствующий парсер
    // Поддерживаются показания датчиков, состояния реле и настройки Auto Mode
    private fun parseIncomingData(line: String) {
        try {
            val json = JSONObject(line)
            when {
                json.has("t") || json.has("h") || json.has("s") || json.has("l") -> parseSensors(json)
                json.has("relays") -> parseRelays(json.getJSONObject("relays"))
                json.has("auto") -> parseAutoMode(json.getJSONObject("auto"))
                else -> Log.d(TAG, "Получена неизвестная JSON-строка: $line")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Получена неизвестная строка: $line")
        }
    }

    // Парсит JSON с показаниями датчиков
    private fun parseSensors(json: JSONObject) {
        val sensors = CurrentSensors(
            temperature = if (json.has("t")) json.getDouble("t") else null,
            airHumidity = if (json.has("h")) json.getDouble("h") else null,
            soilMoisture = if (json.has("s")) json.getDouble("s") else null,
            light = if (json.has("l")) json.getDouble("l") else null
        )
        _currentSensors.value = sensors
    }

    // Парсит JSON с состояниями реле
    private fun parseRelays(json: JSONObject) {
        val keys = listOf("fan", "pump", "lamp")
        val relays = keys.mapNotNull { key ->
            val relayJson = json.optJSONObject(key) ?: return@mapNotNull null
            RelayUiItem(
                key = key,
                titleRes = relayTitleResByKey(key),
                modeAuto = relayJson.getBoolean("auto"),
                state = relayJson.getBoolean("state")
            )
        }
        if (relays.size == keys.size) {
            _relayStates.value = relays
        }
    }

    // Парсит JSON с настройками Auto Mode
    private fun parseAutoMode(json: JSONObject) {
        val lightMode = json.getBoolean("lightMode")
        val (startHour, startMinute) = parseTime(json.optString("lightStart", "00:00"))
        val (endHour, endMinute) = parseTime(json.optString("lightEnd", "00:00"))

        _autoModeSettings.value = ParsedAutoModeSettings(
            temperatureMin = json.getInt("tMin"),
            temperatureMax = json.getInt("tMax"),
            soilMoistureMin = json.getInt("sMin"),
            lightMin = json.getInt("lMin"),
            lightMax = json.getInt("lMax"),
            lightingScheduleMode = lightMode,
            lightStartHour = startHour,
            lightStartMinute = startMinute,
            lightEndHour = endHour,
            lightEndMinute = endMinute
        )
    }

    // Разбирает строку времени формата "HH:MM" на часы и минуты
    private fun parseTime(time: String): Pair<Int, Int> {
        return try {
            val parts = time.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    // Закрывает сокет и потоки, отменяет чтение
    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null

        try {
            reader?.close()
        } catch (_: IOException) {
            // ignore
        }
        reader = null

        try {
            outputStream?.close()
        } catch (_: IOException) {
            // ignore
        }
        outputStream = null

        try {
            bluetoothSocket?.close()
        } catch (_: IOException) {
            // ignore
        }
        bluetoothSocket = null
    }

    companion object {
        // Стандартный UUID для профиля Serial Port Profile (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Имя Bluetooth-устройства ESP32, по которому оно ищется среди сопряжённых
        const val ESP_DEVICE_NAME = "ESP32_Microclimate"

        private const val TAG = "BluetoothRepository"
    }
}

// Сериализует команду в JSON-строку
// Добавляет поле "cmd" и все публичные поля data-класса
private fun BluetoothCommand.toJson(): String {
    val json = JSONObject()
    json.put("cmd", this.cmd)

    when (this) {
        is BluetoothCommand.RequestSensors,
        is BluetoothCommand.GetRelays,
        is BluetoothCommand.GetAutoMode -> {
            // Дополнительных полей нет
        }
        is BluetoothCommand.SetTime -> {
            json.put("unixTime", unixTime)
        }
        is BluetoothCommand.SetRelay -> {
            json.put("relay", relay)
            json.put("state", state)
            json.put("auto", auto)
        }
        is BluetoothCommand.SetAutoModeSettings -> {
            json.put("tMin", temperatureMin)
            json.put("tMax", temperatureMax)
            json.put("sMin", soilMoistureMin)
            json.put("lMin", lightMin)
            json.put("lMax", lightMax)
            json.put("lightMode", lightingScheduleMode)
            json.put("lightStart", String.format(java.util.Locale.US, "%02d:%02d", lightStartHour, lightStartMinute))
            json.put("lightEnd", String.format(java.util.Locale.US, "%02d:%02d", lightEndHour, lightEndMinute))
        }
    }

    return json.toString()
}
