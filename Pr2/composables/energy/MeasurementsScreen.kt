package com.tetiana.wind_speed_monitoring.composables.energy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetiana.wind_speed_monitoring.components.*
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementFilter
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementSort
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementStatus
import com.tetiana.wind_speed_monitoring.viewmodel.MeasurementsContent
import com.tetiana.wind_speed_monitoring.viewmodel.MeasurementsViewModel
import com.tetiana.wind_speed_monitoring.viewmodel.state.OperationState
import com.tetiana.wind_speed_monitoring.viewmodel.state.UiState

// Пороги фільтра мінімальної швидкості вітру
private val WIND_THRESHOLDS = listOf(0.0, 5.0, 10.0, 15.0)

// Дії журналу вимірювань одним об'єктом
data class MeasurementCallbacks(
    val onStatusChange: (MeasurementStatus?) -> Unit,
    val onMinWindChange: (Double) -> Unit,
    val onSortChange: (MeasurementSort) -> Unit,
    val onFetchSensor: () -> Unit,
    val onForceRefresh: () -> Unit,
    val onDismissSensor: () -> Unit,
    val onEdit: (MeasurementEntity) -> Unit,
    val onDelete: (MeasurementEntity) -> Unit
)

//    Окремий маршрут для телефона (Compact)
@Composable
fun MeasurementsScreen(turbineId: Long, onNavigateBack: () -> Unit) {
    MeasurementsPane(turbineId = turbineId, onBack = onNavigateBack, modifier = Modifier.fillMaxSize())
}

//
//  Панель журналу вимірювань - повторно використовується і як окремий екран
//  (телефон), і як права панель list-detail (планшет).
//  key = turbineId: кожна турбіна має власну MeasurementsViewModel
//
@Composable
fun MeasurementsPane(turbineId: Long, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val viewModel: MeasurementsViewModel = viewModel(
        key = "measurements_$turbineId",
        factory = MeasurementsViewModel.factory(turbineId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val callbacks = MeasurementCallbacks(
        onStatusChange = viewModel::onStatusFilterChange,
        onMinWindChange = viewModel::onMinWindChange,
        onSortChange = viewModel::onSortChange,
        onFetchSensor = { viewModel.fetchFromSensor(forceRefresh = false) },
        onForceRefresh = { viewModel.fetchFromSensor(forceRefresh = true) },
        onDismissSensor = viewModel::clearSensorState,
        onEdit = viewModel::openEditForm,
        onDelete = viewModel::requestDelete
    )

    // Вбудована панель не додає системні відступи вдруге (їх уже врахував зовнішній Scaffold)
    val isEmbedded = onBack == null
    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (isEmbedded) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is UiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::openAddForm,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Додати") }
                )
            }
        }
    ) { innerPadding ->
        AdaptiveScreen(modifier = Modifier.padding(innerPadding)) { paneType ->
            StateContent(state = uiState, onRetry = viewModel::retry) { content ->
                MeasurementsList(
                    content = content,
                    filter = filter,
                    sensorState = sensorState,
                    wide = paneType != WindowType.Compact,
                    onBack = onBack,
                    callbacks = callbacks
                )
            }
        }
    }

    form?.let { state ->
        UniversalFormDialog(
            state = state,
            onValueChange = viewModel::onFormValueChange,
            onOptionSelect = { _, _ -> },
            onConfirm = viewModel::saveForm,
            onDismiss = viewModel::dismissForm
        )
    }
    pendingDelete?.let { m ->
        ConfirmDeleteDialog(
            title = "Видалити вимірювання?",
            message = "${m.timestamp.toDateTimeText()}: ${m.windSpeed} м/с → ${m.powerKw} кВт",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }
}

//    Вміст журналу. Шапка (заголовок, числа, сенсор, фільтри) - один блок
//    з рівними відступами, далі - простий список записів з розділювачами
@Composable
fun MeasurementsList(
    content: MeasurementsContent,
    filter: MeasurementFilter,
    sensorState: OperationState,
    wide: Boolean,
    onBack: (() -> Unit)?,
    callbacks: MeasurementCallbacks
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScreenTopBar(
                    title = content.turbine.name,
                    subtitle = turbineSubtitle(content.turbine),
                    onBack = onBack
                )
                StatStrip(
                    listOf(
                        StatItem("записів", content.stats.count.toString()),
                        StatItem("пік", "${content.stats.maxPowerKw.formatted(0)} кВт"),
                        StatItem("енергія", energyText(content.stats.energyKwh))
                    )
                )
                SensorCard(
                    sensorState = sensorState,
                    onFetch = callbacks.onFetchSensor,
                    onForceRefresh = callbacks.onForceRefresh,
                    onDismiss = callbacks.onDismissSensor
                )
                Column {
                    SectionLabel("Журнал вимірювань")
                    MeasurementFiltersSection(filter, callbacks)
                }
            }
        }

        if (content.measurements.isEmpty()) {
            item { EmptyView("Немає вимірювань за обраними фільтрами", Modifier.fillMaxWidth()) }
        } else {
            items(content.measurements, key = { it.id }) { m ->
                MeasurementRow(
                    measurement = m,
                    wide = wide,
                    onEdit = { callbacks.onEdit(m) },
                    onDelete = { callbacks.onDelete(m) }
                )
            }
        }
    }
}

// Параметри турбіни одним рядком замість трьох міток
private fun turbineSubtitle(turbine: TurbineEntity): String =
    "R ${turbine.bladeRadius.formatted(0)} м · ККД ${turbine.efficiency.formatted(0)} % · ${turbine.ratedPowerKw.formatted(0)} кВт"

//    Компактна картка анемометра: опис + одна кнопка.
//    Результат (успіх / помилка) з'являється під нею лише після запиту
@Composable
fun SensorCard(
    sensorState: OperationState,
    onFetch: () -> Unit,
    onForceRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val isRunning = sensorState is OperationState.Running
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Анемометр", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Показання додається в журнал",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onFetch, enabled = !isRunning) {
                    if (isRunning) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Опитати")
                    }
                }
            }
            if (sensorState is OperationState.Success || sensorState is OperationState.Error) {
                Spacer(Modifier.height(12.dp))
                OperationStatusBanner(state = sensorState, onDismiss = onDismiss)
            }
            if (sensorState is OperationState.Success) {
                LinkTextButton(text = "Оновити без кешу", onClick = onForceRefresh)
            }
        }
    }
}

//    Фільтри згорнуті в один рядок; сортування завжди під рукою справа
@Composable
fun MeasurementFiltersSection(filter: MeasurementFilter, callbacks: MeasurementCallbacks) {
    val activeCount = (if (filter.status != null) 1 else 0) + (if (filter.minWindSpeed > 0) 1 else 0)
    CollapsibleFilters(
        activeCount = activeCount,
        trailing = {
            SortDropdown(
                options = MeasurementSort.values().toList(),
                selected = filter.sort,
                label = { it.label },
                onSelect = callbacks.onSortChange
            )
        }
    ) {
        FilterChipsRow(
            options = listOf<MeasurementStatus?>(null) + MeasurementStatus.values(),
            selected = filter.status,
            label = { it?.label ?: "Усі статуси" },
            onSelect = callbacks.onStatusChange
        )
        FilterChipsRow(
            options = WIND_THRESHOLDS,
            selected = filter.minWindSpeed,
            label = { if (it == 0.0) "Будь-який вітер" else "≥ ${it.toInt()} м/с" },
            onSelect = callbacks.onMinWindChange
        )
    }
}

// Колір статусу: генерація - основний, штиль - нейтральний, шторм - червоний
@Composable
fun statusColor(status: MeasurementStatus): Color = when (status) {
    MeasurementStatus.GENERATING -> MaterialTheme.colorScheme.primary
    MeasurementStatus.CALM -> MaterialTheme.colorScheme.outline
    MeasurementStatus.STORM -> MaterialTheme.colorScheme.error
}

//    Рядок журналу: кольорова точка статусу, головне значення,
//    дата і статус дрібніше. На широкій панелі справа ще й джерело даних
@Composable
fun MeasurementRow(
    measurement: MeasurementEntity,
    wide: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = statusColor(measurement.status)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${measurement.windSpeed.formatted(1)} м/с  →  ${measurement.powerKw.formatted(0)} кВт",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${measurement.timestamp.toDateTimeText()} · ${measurement.status.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (wide) {
                Text(
                    measurement.source.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ItemOverflowMenu(onEdit = onEdit, onDelete = onDelete)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}