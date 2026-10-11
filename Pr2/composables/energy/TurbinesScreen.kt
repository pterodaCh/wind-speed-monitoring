package com.tetiana.wind_speed_monitoring.composables.energy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetiana.wind_speed_monitoring.components.*
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity
import com.tetiana.wind_speed_monitoring.data.local.relation.TurbineSummary
import com.tetiana.wind_speed_monitoring.domain.model.FarmReport
import com.tetiana.wind_speed_monitoring.domain.model.TurbineFilter
import com.tetiana.wind_speed_monitoring.domain.model.TurbineSort
import com.tetiana.wind_speed_monitoring.viewmodel.TurbinesContent
import com.tetiana.wind_speed_monitoring.viewmodel.TurbinesViewModel

// Усі дії списку турбін одним об'єктом - замість десятка параметрів у кожній функції
data class TurbineListActions(
    val onSelect: (Long) -> Unit,
    val onSearchChange: (String) -> Unit,
    val onFarmChange: (Long?) -> Unit,
    val onSortChange: (TurbineSort) -> Unit,
    val onAdd: () -> Unit,
    val onEdit: (TurbineSummary) -> Unit,
    val onDelete: (TurbineSummary) -> Unit,
    val onBack: () -> Unit
)

//
//  Екран турбін (шар View в MVVM).
//  Лише відображає стан з TurbinesViewModel і передає їй дії користувача.
//  Адаптивність:
//   Compact          - список; натискання відкриває окремий екран вимірювань;
//   Medium/Expanded  - list-detail: список ліворуч, журнал обраної турбіни праворуч
//
@Composable
fun TurbinesScreen(
    onOpenTurbine: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TurbinesViewModel = viewModel(factory = TurbinesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedTurbineId.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Разові події з ViewModel -> Snackbar
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        AdaptiveScreen(modifier = Modifier.padding(innerPadding)) { windowType ->
            StateContent(state = uiState, onRetry = viewModel::retry) { content ->
                val isCompact = windowType == WindowType.Compact
                val actions = TurbineListActions(
                    onSelect = { id -> if (isCompact) onOpenTurbine(id) else viewModel.selectTurbine(id) },
                    onSearchChange = viewModel::onSearchChange,
                    onFarmChange = viewModel::onFarmFilterChange,
                    onSortChange = viewModel::onSortChange,
                    onAdd = viewModel::openAddForm,
                    onEdit = viewModel::openEditForm,
                    onDelete = viewModel::requestDelete,
                    onBack = onNavigateBack
                )
                if (isCompact) {
                    TurbinesListPane(content, filter, selectedId = null, actions, Modifier.fillMaxSize())
                } else {
                    Row(Modifier.fillMaxSize()) {
                        TurbinesListPane(content, filter, selectedId, actions, Modifier.weight(1f).fillMaxHeight())
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TurbineDetailPane(selectedId, Modifier.weight(1.2f).fillMaxHeight())
                    }
                }
            }
        }
    }

    form?.let { state ->
        UniversalFormDialog(
            state = state,
            onValueChange = viewModel::onFormValueChange,
            onOptionSelect = viewModel::onFormOptionSelect,
            onConfirm = viewModel::saveForm,
            onDismiss = viewModel::dismissForm
        )
    }
    pendingDelete?.let { turbine ->
        ConfirmDeleteDialog(
            title = "Видалити турбіну?",
            message = "«${turbine.name}» і всі її вимірювання (${turbine.measurementCount}) буде видалено з бази даних.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }
}

//    Права панель планшетного режиму: журнал обраної турбіни або підказка
@Composable
fun TurbineDetailPane(selectedId: Long?, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (selectedId == null) {
            EmptyView(
                message = "Оберіть турбіну зі списку,\nщоб переглянути журнал вимірювань",
                icon = Icons.Filled.TouchApp,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            key(selectedId) {
                MeasurementsPane(turbineId = selectedId, onBack = null, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

//    Список турбін. Порядок блоків - від загального до деталей:
//    заголовок → 3 ключові числа → станції → пошук і фільтри → турбіни
@Composable
fun TurbinesListPane(
    content: TurbinesContent,
    filter: TurbineFilter,
    selectedId: Long?,
    actions: TurbineListActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenTopBar(
                title = "Турбіни",
                subtitle = "${content.fleet.turbineCount} турбін · ${content.reports.size} станції",
                onBack = actions.onBack
            ) {
                FilledTonalIconButton(onClick = actions.onAdd) {
                    Icon(Icons.Filled.Add, "Додати турбіну")
                }
            }
        }
        item {
            StatStrip(
                listOf(
                    StatItem("вироблено", energyText(content.fleet.totalEnergyKwh)),
                    StatItem("сер. вітер", "${content.fleet.avgWindSpeed.formatted(1)} м/с"),
                    StatItem("вимірювань", content.fleet.measurementCount.toString())
                )
            )
        }

        item { SectionLabel("Станції") }
        item { FarmReportsRow(content.reports) }

        item { SectionLabel("Турбіни") }
        item { TurbineFiltersSection(filter, content.farms, actions) }

        if (content.turbines.isEmpty()) {
            item { EmptyView("За обраними фільтрами турбін не знайдено", Modifier.fillMaxWidth()) }
        } else {
            items(content.turbines, key = { it.id }) { turbine ->
                TurbineCard(
                    turbine = turbine,
                    selected = turbine.id == selectedId,
                    onClick = { actions.onSelect(turbine.id) },
                    onEdit = { actions.onEdit(turbine) },
                    onDelete = { actions.onDelete(turbine) }
                )
            }
        }
    }
}

//    Горизонтальна стрічка звітів по станціях (зв'язок FarmWithTurbines)
@Composable
fun FarmReportsRow(reports: List<FarmReport>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(reports, key = { it.farmId }) { report -> FarmReportCard(report) }
    }
}

//    Картка станції: назва, вироблена енергія і лідер - без зайвих деталей
@Composable
fun FarmReportCard(report: FarmReport) {
    OutlinedCard(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                report.farmName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(report.region, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(
                energyText(report.energyKwh),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                buildString {
                    append("${report.turbineCount} турбін")
                    report.bestTurbineName?.let { append(" · лідер $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//    Пошук завжди видно; фільтр станції - у згорнутій панелі; сортування - справа
@Composable
fun TurbineFiltersSection(filter: TurbineFilter, farms: List<WindFarmEntity>, actions: TurbineListActions) {
    Column {
        AppTextField(
            value = filter.query,
            onValueChange = actions.onSearchChange,
            label = "Пошук турбіни",
            imeAction = ImeAction.Done
        )
        CollapsibleFilters(
            activeCount = if (filter.farmId != null) 1 else 0,
            trailing = {
                SortDropdown(
                    options = TurbineSort.values().toList(),
                    selected = filter.sort,
                    label = { it.label },
                    onSelect = actions.onSortChange
                )
            }
        ) {
            FilterChipsRow(
                options = listOf<WindFarmEntity?>(null) + farms,
                selected = farms.firstOrNull { it.id == filter.farmId },
                label = { it?.name ?: "Усі станції" },
                onSelect = { actions.onFarmChange(it?.id) }
            )
        }
    }
}

//    Картка турбіни.
//    Ліворуч - назва, станція і один рядок деталей; праворуч - головне число (енергія).
//    Дії сховані в меню ⋮. КВВП - коефіцієнт використання встановленої потужності
@Composable
fun TurbineCard(
    turbine: TurbineSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(turbine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(turbine.farmName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${turbine.avgWindSpeed.formatted(1)} м/с · КВВП ${turbine.capacityFactor.formatted(0)} % · ${turbine.measurementCount} зап.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    energyText(turbine.energyKwh),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("вироблено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ItemOverflowMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}