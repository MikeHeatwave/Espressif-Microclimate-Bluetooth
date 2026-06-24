package com.example.espressifmicroclimate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressifmicroclimate.MicroclimateApplication
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.LoadState
import com.example.espressifmicroclimate.ui.components.ModeSelectionRow
import com.example.espressifmicroclimate.ui.components.NumericInputField
import com.example.espressifmicroclimate.ui.components.SettingsCard
import com.example.espressifmicroclimate.ui.components.TimePickerRow
import com.example.espressifmicroclimate.ui.viewmodel.AutoModeViewModel

/**
 * Экран редактирования параметров Auto Mode
 * Позволяет задать пороги температуры, влажности почвы, а также режим освещения: пороговый или по расписанию
 *
 * При нажатии "Применить параметры Auto Mode" настройки отправляются на ESP32 по Bluetooth
 * и сохраняются в SharedPreferences для восстановления при следующем запуске приложения
 */
@Composable
fun AutoModeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as MicroclimateApplication
    val viewModel: AutoModeViewModel = viewModel(
        factory = AutoModeViewModel.createFactory(
            repository = application.bluetoothRepository,
            context = context.applicationContext
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val fixFieldsText = stringResource(R.string.auto_mode_error_fix_fields)

    // При каждом входе на экран проверяем не была ли выбрана предустановка
    LaunchedEffect(Unit) {
        viewModel.applyPendingPresetIfNeeded()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Статичный заголовок экрана
        Text(
            text = stringResource(R.string.auto_mode_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Карточка с полями температуры
            item {
                SettingsCard(title = stringResource(R.string.auto_mode_temperature_card)) {
                    NumericInputField(
                        value = uiState.editorState.temperatureMin,
                        onValueChange = {
                            viewModel.updateEditor { state -> state.copy(temperatureMin = it) }
                        },
                        label = stringResource(R.string.auto_mode_temperature_min),
                        errorRes = uiState.validation.temperatureMinError
                    )
                    NumericInputField(
                        value = uiState.editorState.temperatureMax,
                        onValueChange = {
                            viewModel.updateEditor { state -> state.copy(temperatureMax = it) }
                        },
                        label = stringResource(R.string.auto_mode_temperature_max),
                        errorRes = uiState.validation.temperatureMaxError
                    )
                }
            }

            // Карточка с минимальной влажностью почвы
            item {
                SettingsCard(title = stringResource(R.string.auto_mode_soil_card)) {
                    NumericInputField(
                        value = uiState.editorState.soilMoistureMin,
                        onValueChange = {
                            viewModel.updateEditor { state -> state.copy(soilMoistureMin = it) }
                        },
                        label = stringResource(R.string.auto_mode_soil_min),
                        errorRes = uiState.validation.soilMoistureMinError
                    )
                }
            }

            // Карточка освещения: выбор режима и соответствующие поля
            item {
                SettingsCard(title = stringResource(R.string.auto_mode_light_card)) {
                    Text(
                        text = stringResource(R.string.auto_mode_mode_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    ModeSelectionRow(
                        selected = !uiState.editorState.lightingScheduleMode,
                        title = stringResource(R.string.auto_mode_light_threshold_mode),
                        onClick = {
                            viewModel.updateEditor { state -> state.copy(lightingScheduleMode = false) }
                        }
                    )

                    ModeSelectionRow(
                        selected = uiState.editorState.lightingScheduleMode,
                        title = stringResource(R.string.auto_mode_light_schedule_mode),
                        onClick = {
                            viewModel.updateEditor { state -> state.copy(lightingScheduleMode = true) }
                        }
                    )

                    if (!uiState.editorState.lightingScheduleMode) {
                        NumericInputField(
                            value = uiState.editorState.lightMin,
                            onValueChange = {
                                viewModel.updateEditor { state -> state.copy(lightMin = it) }
                            },
                            label = stringResource(R.string.auto_mode_light_min),
                            errorRes = uiState.validation.lightMinError
                        )
                        NumericInputField(
                            value = uiState.editorState.lightMax,
                            onValueChange = {
                                viewModel.updateEditor { state -> state.copy(lightMax = it) }
                            },
                            label = stringResource(R.string.auto_mode_light_max),
                            errorRes = uiState.validation.lightMaxError
                        )
                    } else {
                        TimePickerRow(
                            title = stringResource(R.string.auto_mode_schedule_start),
                            hour = uiState.editorState.lightStartHour,
                            minute = uiState.editorState.lightStartMinute,
                            onHourSelected = {
                                viewModel.updateEditor { state -> state.copy(lightStartHour = it) }
                            },
                            onMinuteSelected = {
                                viewModel.updateEditor { state -> state.copy(lightStartMinute = it) }
                            }
                        )
                        TimePickerRow(
                            title = stringResource(R.string.auto_mode_schedule_end),
                            hour = uiState.editorState.lightEndHour,
                            minute = uiState.editorState.lightEndMinute,
                            onHourSelected = {
                                viewModel.updateEditor { state -> state.copy(lightEndHour = it) }
                            },
                            onMinuteSelected = {
                                viewModel.updateEditor { state -> state.copy(lightEndMinute = it) }
                            }
                        )
                    }
                }
            }
        }

        // Кнопка сохранения активна только при наличии изменений и корректно заполненных полях
        Button(
            onClick = { viewModel.save(fixFieldsText) },
            enabled = uiState.loadState != LoadState.Loading &&
                uiState.hasPendingChanges &&
                uiState.validation.isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.save_auto_mode))
        }
    }
}
