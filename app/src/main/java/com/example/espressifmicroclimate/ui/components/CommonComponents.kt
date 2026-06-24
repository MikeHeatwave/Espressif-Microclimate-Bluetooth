package com.example.espressifmicroclimate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.espressifmicroclimate.R
import com.example.espressifmicroclimate.data.model.MetricCardData
import com.example.espressifmicroclimate.data.model.RelayUiItem
import com.example.espressifmicroclimate.utils.twoDigits

// Цвета карточек панелей
@Composable
fun panelCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
)

// Граница карточек панелей, используется почти на всех экранах
@Composable
fun panelCardBorder(): BorderStroke = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant
)

// Карточка-контейнер с заголовком и произвольным содержимым
// Используется в экранах Auto Mode и Settings
@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        border = panelCardBorder(),
        colors = panelCardColors()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp
            )
            content()
        }
    }
}

// Строка с радиокнопкой для выбора режима темы
@Composable
fun ThemeModeRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}


// Числовое поле ввода с фильтрацией нецифровых символов
// При наличии ошибки отображает supporting text с её описанием
@Composable
fun NumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorRes: Int?
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() }) },
        label = { Text(text = label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = errorRes != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        ),
        supportingText = {
            if (errorRes != null) {
                Text(text = stringResource(errorRes))
            }
        }
    )
}

// Строка с радиокнопкой для выбора режима работы освещения
@Composable
fun ModeSelectionRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

// Блок выбора времени (часы + минуты) через DropdownMenu
// Используется в редакторе расписания освещения
@Composable
fun TimePickerRow(
    title: String,
    hour: Int,
    minute: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberDropdown(
                label = stringResource(R.string.auto_mode_hour),
                value = hour,
                range = 0..23,
                onSelected = onHourSelected,
                modifier = Modifier.weight(1f)
            )
            NumberDropdown(
                label = stringResource(R.string.auto_mode_minute),
                value = minute,
                range = 0..59,
                onSelected = onMinuteSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Выпадающий список чисел в заданном диапазоне с двузначным форматированием
@Composable
fun NumberDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = value.twoDigits())
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                range.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.twoDigits()) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Карточка одного реле с переключателями режима и состояния
@Composable
fun RelayCard(
    relay: RelayUiItem,
    onAutoModeChanged: (Boolean) -> Unit,
    onStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = panelCardBorder(),
        colors = panelCardColors()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(relay.titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.relay_key_label, relay.key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Переключатель автоматического режима
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.relay_auto_mode),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (relay.modeAuto) {
                            stringResource(R.string.relay_auto_on)
                        } else {
                            stringResource(R.string.relay_auto_off)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = relay.modeAuto,
                    onCheckedChange = onAutoModeChanged
                )
            }

            // Переключатель состояния, блокируется в автоматическом режиме
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.relay_state),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (relay.state) {
                            stringResource(R.string.relay_state_on)
                        } else {
                            stringResource(R.string.relay_state_off)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = relay.state,
                    onCheckedChange = onStateChanged,
                    enabled = !relay.modeAuto
                )
            }

            // Подсказка, когда ручное управление недоступно
            if (relay.modeAuto) {
                Text(
                    text = stringResource(R.string.relay_manual_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Карточка показателя на главном экране: заголовок и крупное значение
@Composable
fun MetricCard(data: MetricCardData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(170.dp),
        border = panelCardBorder(),
        colors = panelCardColors()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(data.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.value,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
