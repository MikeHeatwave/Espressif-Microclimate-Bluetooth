#include <Arduino.h>
#include <Wire.h>
#include <ArduinoJson.h>
#include <Adafruit_SHT31.h>
#include <BluetoothSerial.h>
#include <math.h>
#include <time.h>
#include <string.h>
#include <stdlib.h>

namespace {
constexpr uint8_t I2C_SDA_PIN = 21;
constexpr uint8_t I2C_SCL_PIN = 22;
constexpr uint8_t SHT31_I2C_ADDRESS = 0x44; // Адрес датчика SHT31 на шине I2C
float temperatureC = 0.0f;
float airHumidity = 0.0f;
Adafruit_SHT31 sht31;

constexpr uint8_t SOIL_MOISTURE_PIN = 32;
constexpr int16_t AIR_VALUE = 2500; // Показание датчика в воздухе/сухой почве
constexpr int16_t WATER_VALUE = 850; // Показание датчика в воде/очень влажной почве
int16_t soilMoistureRawValue = 0;
int16_t soilMoisturePercent = 0;

constexpr uint8_t PHOTORESISTOR_PIN = 34;
constexpr int16_t ADC_MAX_VALUE = 4095; // Максимальное значение в 12-битном АЦП
constexpr float ADC_REFERENCE_VOLTAGE = 3.3f; // Максимальное напряжение от фоторезистора
constexpr float SERIES_RESISTOR_OHMS = 10000.0f; // Делитель напряжения (резистор 10 кОм)
constexpr float LDR_GAMMA = 0.7f; // Наклон логарифмической характеристики
constexpr float LDR_RL10_KOHM = 50.0f; // Сопротивление фоторезистора при 10 lux
int16_t photoRawValue = 0;
float lux = 0.0f;

BluetoothSerial SerialBT;
constexpr const char* DEVICE_NAME = "ESP32_Microclimate";

constexpr uint8_t LAMP_RELAY_PIN = 16;
constexpr uint8_t PUMP_RELAY_PIN = 17;
constexpr uint8_t FAN_RELAY_PIN = 18;
bool fanState = false;
bool pumpState = false;
bool lampState = false;
bool fanModeAuto = false;
bool pumpModeAuto = false;
bool lampModeAuto = false;

float temperatureMax = 0.0f; // Пороги (гистерезис) параметров
float temperatureMin = 0.0f;
int16_t soilMoistureMin = 0;
float lightMin = 0.0f;
float lightMax = 0.0f;
int16_t lightStartHour = 0;
int16_t lightStartMinute = 0;
int16_t lightEndHour = 0;
int16_t lightEndMinute = 0;
bool lightingScheduleMode = false;

bool fanRunning = false;
uint32_t lastFanTime = 0;
constexpr uint32_t FAN_WORK_TIME = 30000; // 30 секунд - Время работы вентилятора
uint32_t lastPumpTime = 0;
constexpr uint32_t PUMP_WORK_TIME = 10000; // 10 секунд - Время работы помпы полива
constexpr uint32_t PUMP_INTERVAL = 1200000; // 20 минут - Интервал между поливами
bool lampShouldBeOn = false;

uint32_t baseUnixTime = 0; // Unix timestamp в секундах в момент синхронизации
uint32_t baseMillis = 0; // Значение millis() в момент синхронизации
bool timeSynchronized = false;

uint32_t currentLoopTime = 0;
uint32_t lastMainLoopTime = 0;
constexpr uint32_t MAIN_INTERVAL = 2000; // 2 секунды - Период чтения датчиков
uint32_t lastProcessAutoModeTime = 0;
constexpr uint32_t PROCESS_AUTO_MODE_INTERVAL = 10000; // 10 секунд - Период выполнения автоматического режима
}

float calculateIlluminance(int16_t photoRawValue) {
  if (photoRawValue <= 0 || photoRawValue >= ADC_MAX_VALUE) {
    return 0.0f;
  }
  float photoVoltage = photoRawValue * (ADC_REFERENCE_VOLTAGE / ADC_MAX_VALUE); // Перевод значения с АЦП в значение напряжения
  float ldrResistance = SERIES_RESISTOR_OHMS * (ADC_REFERENCE_VOLTAGE - photoVoltage) / photoVoltage; // Вычисление сопротивления падающего на фоторезисторе
  float illuminance = pow((LDR_RL10_KOHM * 1000.0f * pow(10.0f, LDR_GAMMA)) / ldrResistance, 1.0f / LDR_GAMMA); // Формула вычисления освещённости
  if (!isfinite(illuminance) || illuminance < 0.0f) {
    return 0.0f;
  }
  return illuminance;
}

void readSensors() {
  temperatureC = sht31.readTemperature();
  airHumidity = sht31.readHumidity();

  soilMoistureRawValue = analogRead(SOIL_MOISTURE_PIN);
  soilMoisturePercent = map(soilMoistureRawValue, AIR_VALUE, WATER_VALUE, 0, 100); // Линейный перевод значения с АЦП из первого диапазона в диапазон 0-100
  soilMoisturePercent = constrain(soilMoisturePercent, 0, 100); // Ограничение значения в диапазон 0-100

  photoRawValue = analogRead(PHOTORESISTOR_PIN);
  lux = calculateIlluminance(photoRawValue);
}

// Возвращает текущее Unix-время в секундах
uint32_t getCurrentUnixTime() {
  if (!timeSynchronized) {
    return 0;
  }
  return baseUnixTime + (millis() - baseMillis) / 1000;
}

// Возвращает структуру tm с текущим локальным временем
bool getLocalTimeInfo(struct tm* timeinfo) {
  uint32_t unixTime = getCurrentUnixTime();
  if (unixTime == 0) {
    return false;
  }
  time_t now = unixTime;
  localtime_r(&now, timeinfo);
  return true;
}

void processAutoMode() {
  uint32_t currentMillis = millis();
  if (fanModeAuto == true) {
    if (!fanRunning && temperatureC > temperatureMax) { // Запуск вентилятора если он не работает и температура выше верхнего порога
      fanState = true; // Реле замкнёт контакт
      fanRunning = true; // Флаг запущенного устройства
      lastFanTime = currentMillis;
    }
    if (fanRunning && (currentMillis - lastFanTime >= FAN_WORK_TIME || temperatureC < temperatureMin)) { // Остановка работающего вентилятора если температура упала ниже нижнего порога или время превысило FAN_WORK_TIME
      fanState = false; // Реле разомкнёт контакт
      fanRunning = false;
    }
  }

  if (pumpModeAuto == true) {
    if (soilMoisturePercent < soilMoistureMin && (lastPumpTime == 0 || currentMillis - lastPumpTime >= PUMP_INTERVAL)) { // Запуск помпы если влажность упала ниже нижнего порога и прошёл интервал PUMP_INTERVAL между поливами
      pumpState = true;
      lastPumpTime = currentMillis;
    }
    if (pumpState && currentMillis - lastPumpTime >= PUMP_WORK_TIME) {
      pumpState = false;
    }
  }

  if (lampModeAuto == true) {
    if (lightingScheduleMode == true) { // Для режима освещения по расписанию
      struct tm timeinfo;
      if (!getLocalTimeInfo(&timeinfo)) {
        Serial.println("Time not synchronized");
        return;
      }
      int16_t currentMinutes = timeinfo.tm_hour * 60 + timeinfo.tm_min; // Перевод текущего времени в минуты
      int16_t startMinutes = lightStartHour * 60 + lightStartMinute; // Перевод времени начала досветки в минуты
      int16_t endMinutes = lightEndHour * 60 + lightEndMinute; // Перевод времени окончания досветки в минуты

      lampShouldBeOn = false;
      if (startMinutes < endMinutes) { // Если интервал расписания НЕ проходит через полночь
        if (currentMinutes >= startMinutes && currentMinutes < endMinutes) {
          lampShouldBeOn = true;
        }
      }
      else { // Если интервал расписания проходит через полночь
        if (currentMinutes >= startMinutes || currentMinutes < endMinutes) {
          lampShouldBeOn = true;
        }
      }
      lampState = lampShouldBeOn;
    }
    else { // Для режима освещения по порогам
      if (lux < lightMin) {
        lampState = true;
      }
      else {
        if (lux > lightMax) {
          lampState = false;
        }
      }
    }
  }
}

void applyRelays() {
  digitalWrite(FAN_RELAY_PIN, fanState ? HIGH : LOW);
  digitalWrite(PUMP_RELAY_PIN, pumpState ? HIGH : LOW);
  digitalWrite(LAMP_RELAY_PIN, lampState ? HIGH : LOW);
}

// {"t":24.5,"h":58.0,"s":67.0,"l":220}
void sendSensors() {
  StaticJsonDocument<128> doc;
  doc["t"] = round(temperatureC * 10.0f) / 10.0f;
  doc["h"] = round(airHumidity * 10.0f) / 10.0f;
  doc["s"] = soilMoisturePercent;
  doc["l"] = round(lux * 10.0f) / 10.0f;

  char buffer[128];
  serializeJson(doc, buffer);
  SerialBT.println(buffer);
}

// {"relays":{"fan":{"auto":false,"state":true},"pump":...}}
void sendRelays() {
  StaticJsonDocument<256> doc;
  JsonObject relays = doc.createNestedObject("relays");

  JsonObject fan = relays.createNestedObject("fan");
  fan["auto"] = fanModeAuto;
  fan["state"] = fanState;

  JsonObject pump = relays.createNestedObject("pump");
  pump["auto"] = pumpModeAuto;
  pump["state"] = pumpState;

  JsonObject lamp = relays.createNestedObject("lamp");
  lamp["auto"] = lampModeAuto;
  lamp["state"] = lampState;

  char buffer[256];
  serializeJson(doc, buffer);
  SerialBT.println(buffer);
}

// {"auto":{"tMin":20,"tMax":25,"sMin":70,"lMin":50,"lMax":300,"lightMode":false,"lightStart":"20:00","lightEnd":"08:00"}}
void sendAutoMode() {
  StaticJsonDocument<512> doc;
  JsonObject autoMode = doc.createNestedObject("auto");
  autoMode["tMin"] = static_cast<int16_t>(temperatureMin);
  autoMode["tMax"] = static_cast<int16_t>(temperatureMax);
  autoMode["sMin"] = soilMoistureMin;
  autoMode["lMin"] = static_cast<int16_t>(lightMin);
  autoMode["lMax"] = static_cast<int16_t>(lightMax);
  autoMode["lightMode"] = lightingScheduleMode;

  char lightStart[6];
  snprintf(lightStart, sizeof(lightStart), "%02d:%02d", lightStartHour, lightStartMinute);
  autoMode["lightStart"] = lightStart;

  char lightEnd[6];
  snprintf(lightEnd, sizeof(lightEnd), "%02d:%02d", lightEndHour, lightEndMinute);
  autoMode["lightEnd"] = lightEnd;

  char buffer[512];
  serializeJson(doc, buffer);
  SerialBT.println(buffer);
}

// {"cmd":"setRelay","relay":"pump","state":true,"auto":false}
void handleSetRelay(const JsonObject& doc) {
  const char* relay = doc["relay"] | "";
  bool state = doc["state"] | false;
  bool autoMode = doc["auto"] | false;

  if (strcmp(relay, "fan") == 0) {
    fanState = state;
    fanModeAuto = autoMode;
    if (autoMode) {
      fanRunning = false; // Сбрасываем флаг ручного запуска при переходе в автоматический режим
    }
  }
  else if (strcmp(relay, "pump") == 0) {
    pumpState = state;
    pumpModeAuto = autoMode;
  }
  else if (strcmp(relay, "lamp") == 0) {
    lampState = state;
    lampModeAuto = autoMode;
  }

  if (fanModeAuto || pumpModeAuto || lampModeAuto) {
    processAutoMode(); // Если хотя бы одно реле в автоматическом режиме - сразу пересчитываем логику Auto Mode
  }
  applyRelays();

  sendRelays();
}

// Парсинг строки времени формата "HH:MM"
void parseTimeString(const char* timeStr, int16_t& hour, int16_t& minute) {
  if (timeStr == nullptr || strlen(timeStr) < 5) {
    hour = 0;
    minute = 0;
    return;
  }
  hour = atoi(timeStr); // Функция преобразования строки в число
  minute = atoi(timeStr + 3);
}

// {"cmd":"setAutoModeSettings","tMin":20,"tMax":25,"sMin":70,"lMin":50,"lMax":300,"lightMode":false,"lightStart":"20:00","lightEnd":"08:00"}
void handleSetAutoModeSettings(const JsonObject& doc) {
  temperatureMin = doc["tMin"] | temperatureMin;
  temperatureMax = doc["tMax"] | temperatureMax;
  soilMoistureMin = doc["sMin"] | soilMoistureMin;
  lightMin = doc["lMin"] | lightMin;
  lightMax = doc["lMax"] | lightMax;
  lightingScheduleMode = doc["lightMode"] | lightingScheduleMode;
  const char* lightStart = doc["lightStart"] | "00:00";
  const char* lightEnd = doc["lightEnd"] | "00:00";
  parseTimeString(lightStart, lightStartHour, lightStartMinute);
  parseTimeString(lightEnd, lightEndHour, lightEndMinute);

  processAutoMode(); // При смене настроек Auto Mode сразу определяются и применяются состояния реле, чтобы сразу учитывать новые пороги
  applyRelays();

  sendRelays();
  sendAutoMode();
}

// {"cmd":"setTime","unixTime":1234567890}
void handleSetTime(const JsonObject& doc) {
  uint32_t unixTime = doc["unixTime"] | 0;
  if (unixTime == 0) {
    Serial.println("Invalid setTime command");
    return;
  }
  baseUnixTime = unixTime;
  baseMillis = millis();
  timeSynchronized = true;
  Serial.print("Time synchronized: ");
  Serial.println(baseUnixTime);
}

void processBluetoothCommand(const String& command) {
  Serial.print("Received BT command: ");
  Serial.println(command);

  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, command); // Парсинг входящей JSON-команды от Android
  if (error) {
    Serial.print("JSON parse error: ");
    Serial.println(error.c_str());
    return;
  }

  const char* cmd = doc["cmd"] | "";
  if (strcmp(cmd, "getSensors") == 0) {
    sendSensors(); // Отправить на Android текущие показатели датчиков
  }
  else if (strcmp(cmd, "getRelays") == 0) {
    sendRelays(); // Отправить на Android текущие состояния реле
  }
  else if (strcmp(cmd, "getAutoMode") == 0) {
    sendAutoMode(); // Отправить на Android текущие настройки Auto Mode
  }
  else if (strcmp(cmd, "setRelay") == 0) {
    handleSetRelay(doc.as<JsonObject>()); // Установить состояния реле
  }
  else if (strcmp(cmd, "setAutoModeSettings") == 0) {
    handleSetAutoModeSettings(doc.as<JsonObject>()); // Задать параметры Auto Mode
  }
  else if (strcmp(cmd, "setTime") == 0) {
    handleSetTime(doc.as<JsonObject>()); // Установить текущее время с Android
  }
  else {
    Serial.print("Unknown command: ");
    Serial.println(cmd);
  }
}

void setup() {
  Serial.begin(115200); // Скорость передачи данных в 115200 бод
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN); // Запуск шины I2C на пинах для SHT31
  sht31.begin(SHT31_I2C_ADDRESS); // Инициализация датчика SHT31 по его адресу
  analogReadResolution(12); // Установка точности АЦП в 12 бит (0-4095)
  analogSetAttenuation(ADC_11db); // Ослабление аналогового сигнала для диапазона 0-3.3В
  pinMode(FAN_RELAY_PIN, OUTPUT);
  pinMode(PUMP_RELAY_PIN, OUTPUT);
  pinMode(LAMP_RELAY_PIN, OUTPUT);
  digitalWrite(FAN_RELAY_PIN, LOW);
  digitalWrite(PUMP_RELAY_PIN, LOW);
  digitalWrite(LAMP_RELAY_PIN, LOW);

  delay(1000);
  SerialBT.begin(DEVICE_NAME); // Запуск Bluetooth Serial
  Serial.print("Bluetooth started with name: ");
  Serial.println(DEVICE_NAME);
  Serial.println("---The system has started working---");
}

void loop() {
  currentLoopTime = millis();
  while (SerialBT.available()) {
    String command = SerialBT.readStringUntil('\n'); // Чтение входящих Bluetooth-команд
    command.trim(); // Очистить команду от начальных и конечных пробельных символов
    if (command.length() > 0) {
      processBluetoothCommand(command); // Функция выполнения set/get Bluetooth-команд
    }
  }
  if (currentLoopTime - lastMainLoopTime >= MAIN_INTERVAL) {
    lastMainLoopTime = currentLoopTime;
    readSensors(); // Функция получения параметров микроклимата с датчиков
  }
  if (currentLoopTime - lastProcessAutoModeTime >= PROCESS_AUTO_MODE_INTERVAL) {
    lastProcessAutoModeTime = currentLoopTime;
    processAutoMode(); // Функция выполнения автоматического режима управления устройств
    applyRelays(); // Функция применения состояния реле
  }
}
