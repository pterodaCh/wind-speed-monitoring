package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

//
//  Віджети відображення даних - використовуються на всіх енергетичних екранах.
//  Принцип дизайну: мінімум рамок і кольорів, акцент лише на головних числах
//

// Опис одного показника
data class StatItem(val label: String, val value: String)

//    Стрічка ключових показників: 2–4 числа в один рядок,
//    розділені тонкими лініями. Замінює сітку з окремих плиток -
//    та сама інформація, але візуально легше
@Composable
fun StatStrip(stats: List<StatItem>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(Modifier.padding(vertical = 16.dp).height(IntrinsicSize.Min)) {
            stats.forEachIndexed { index, stat ->
                if (index > 0) {
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stat.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stat.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

//    Приглушений підпис секції ("Станції", "Журнал") з необов'язковою дією справа
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        action()
    }
}

//    Кольорова точка статусу - компактніше за текстову мітку
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

//    Меню дій ⋮ для елемента списку: ховає "Редагувати" і "Видалити",
//    щоб у кожному рядку не було двох яскравих іконок
@Composable
fun ItemOverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, "Дії", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Редагувати") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

// ---- Компоненти попередньої версії (залишені для повторного використання) ----

//    Плитка статистики: підпис + велике значення
@Composable
fun StatTile(item: StatItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                item.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

//    Адаптивна сітка плиток (2 колонки на вузькому екрані, 4 на широкому)
@Composable
fun StatGrid(stats: List<StatItem>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 520.dp) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { StatTile(it, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

//    Маленька кольорова мітка
@Composable
fun BadgeLabel(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(shape = RoundedCornerShape(50), color = containerColor, contentColor = contentColor) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

//    Показник "значення над підписом"
@Composable
fun MetricText(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

//    Кнопки "Редагувати" / "Видалити" у відкритому вигляді
@Composable
fun EditDeleteButtons(onEdit: () -> Unit, onDelete: () -> Unit, enabled: Boolean = true) {
    Row {
        IconButton(onClick = onEdit, enabled = enabled) { Icon(Icons.Filled.Edit, "Редагувати") }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Filled.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ---- Форматування для UI ----

fun Double.formatted(digits: Int = 1): String = String.format(Locale.getDefault(), "%.${digits}f", this)

// Великі значення енергії показуємо в МВт·год
fun energyText(kwh: Double): String =
    if (kwh >= 1000) "${(kwh / 1000).formatted(2)} МВт·год" else "${kwh.formatted(1)} кВт·год"

// Дата й час (дд.мм.рррр гг:хх)
fun Long.toDateTimeText(): String =
    java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(java.util.Date(this))