package com.example.espressifmicroclimate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.ui.components.SettingsCard
import com.example.espressifmicroclimate.ui.components.ThemeModeRow
import com.example.espressifmicroclimate.ui.theme.ThemeMode
import com.example.espressifmicroclimate.utils.saveThemeMode

/**
 * Экран настроек приложения
 * Содержит выбор темы и диалог "О программе"
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Карточка с переключателями светлой/тёмной/системной темы
        SettingsCard(title = stringResource(R.string.settings_theme_title)) {
            ThemeModeRow(
                selected = themeMode == ThemeMode.SYSTEM,
                title = stringResource(R.string.settings_theme_system),
                onClick = {
                    saveThemeMode(context, ThemeMode.SYSTEM)
                    onThemeModeSelected(ThemeMode.SYSTEM)
                }
            )
            ThemeModeRow(
                selected = themeMode == ThemeMode.LIGHT,
                title = stringResource(R.string.settings_theme_light),
                onClick = {
                    saveThemeMode(context, ThemeMode.LIGHT)
                    onThemeModeSelected(ThemeMode.LIGHT)
                }
            )
            ThemeModeRow(
                selected = themeMode == ThemeMode.DARK,
                title = stringResource(R.string.settings_theme_dark),
                onClick = {
                    saveThemeMode(context, ThemeMode.DARK)
                    onThemeModeSelected(ThemeMode.DARK)
                }
            )
        }

        OutlinedButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.settings_about))
        }
    }

    // Диалог "О программном обеспечении"
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = stringResource(R.string.about_dialog_title)) },
            text = { Text(text = stringResource(R.string.about_dialog_text)) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = stringResource(R.string.about_dialog_close))
                }
            }
        )
    }
}
