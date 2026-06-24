package com.example.espressifmicroclimate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.AutoModePreset
import com.example.espressifmicroclimate.data.model.autoModePresets
import com.example.espressifmicroclimate.ui.components.panelCardBorder
import com.example.espressifmicroclimate.ui.components.panelCardColors
import com.example.espressifmicroclimate.ui.viewmodel.PresetsViewModel

/**
 * Экран готовых предустановок Auto Mode
 * Кнопка "Применить для Auto Mode" переходит на экран "Параметры Auto Mode", где параметры пресета вставляются в соответствующие поля
 */
@Composable
fun PresetsScreen(
    onNavigateToAutoMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PresetsViewModel = viewModel(
        factory = PresetsViewModel.createFactory(context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsState()

    // При выборе предустановки инициируем переход на экран Auto Mode
    LaunchedEffect(uiState.navigateToAutoMode) {
        if (uiState.navigateToAutoMode) {
            onNavigateToAutoMode()
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.menu_presets),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(autoModePresets, key = { it.id }) { preset ->
                PresetCard(
                    preset = preset,
                    onApply = { viewModel.applyPreset(preset) }
                )
            }
        }
    }
}

// Карточка одной предустановки с изображением культуры, параметрами и кнопкой "Применить"
@Composable
private fun PresetCard(
    preset: AutoModePreset,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = panelCardBorder(),
        colors = panelCardColors()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(preset.imageRes),
                contentDescription = stringResource(preset.nameRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = stringResource(preset.nameRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )

            // Отображаем температуру и влажность почвы, освещение не меняется предустановкой
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(
                        R.string.preset_temperature,
                        preset.temperatureMin,
                        preset.temperatureMax
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(
                        R.string.preset_soil_moisture,
                        preset.soilMoistureMin
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.presets_apply))
            }
        }
    }
}
