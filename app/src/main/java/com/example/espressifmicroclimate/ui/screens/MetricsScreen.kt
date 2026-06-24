package com.example.espressifmicroclimate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressifmicroclimate.MicroclimateApplication
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.MetricCardData
import com.example.espressifmicroclimate.ui.components.MetricCard
import com.example.espressifmicroclimate.ui.viewmodel.MetricsViewModel
import com.example.espressifmicroclimate.utils.formatMetricValue

/**
 * Главный экран "Показатели"
 * Отображает текущие значения температуры, влажности воздуха, влажности почвы и освещённости, полученные с ESP32 по Bluetooth
 * Обновление данных происходит автоматически каждые 2 секунды через MetricsViewModel
 */
@Composable
fun MetricsScreen(
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as MicroclimateApplication
    val viewModel: MetricsViewModel = viewModel(
        factory = MetricsViewModel.createFactory(application.bluetoothRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val noDataText = stringResource(R.string.metric_no_data)

    // Формируем карточки показателей с единицами измерения
    val metrics = listOf(
        MetricCardData(
            titleRes = R.string.metric_temperature,
            value = formatMetricValue(uiState.sensors.temperature, "°C", noDataText)
        ),
        MetricCardData(
            titleRes = R.string.metric_air_humidity,
            value = formatMetricValue(uiState.sensors.airHumidity, "%", noDataText)
        ),
        MetricCardData(
            titleRes = R.string.metric_soil_humidity,
            value = formatMetricValue(uiState.sensors.soilMoisture, "%", noDataText)
        ),
        MetricCardData(
            titleRes = R.string.metric_light,
            value = formatMetricValue(uiState.sensors.light, "lux", noDataText)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.metrics_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(data = metrics[0], modifier = Modifier.weight(1f))
            MetricCard(data = metrics[1], modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(data = metrics[2], modifier = Modifier.weight(1f))
            MetricCard(data = metrics[3], modifier = Modifier.weight(1f))
        }
    }
}
