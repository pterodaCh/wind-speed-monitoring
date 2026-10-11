package com.tetiana.wind_speed_monitoring.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//
//  Універсальні (generic) елементи фільтрації та сортування
//

//    Згортувана панель фільтрів.
//    У згорнутому стані займає один рядок: "Фільтри · N" + сортування справа.
//    Чипи з'являються лише за потреби - екран не перевантажений
@Composable
fun CollapsibleFilters(
    activeCount: Int,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Filled.FilterList, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (activeCount > 0) "Фільтри · $activeCount" else "Фільтри")
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            Spacer(Modifier.weight(1f))
            trailing()
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

//    Горизонтальний ряд чипів з одиничним вибором
@Composable
fun <T> FilterChipsRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                enabled = enabled,
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

//    Вибір сортування: текстова кнопка з випадним меню (легша за обведену кнопку)
@Composable
fun <T> SortDropdown(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Filled.Check, null) }
                    } else null
                )
            }
        }
    }
}