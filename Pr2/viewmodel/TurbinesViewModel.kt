package com.tetiana.wind_speed_monitoring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tetiana.wind_speed_monitoring.auth.SessionManager
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity
import com.tetiana.wind_speed_monitoring.data.local.relation.TurbineSummary
import com.tetiana.wind_speed_monitoring.data.repository.WindDataRepository
import com.tetiana.wind_speed_monitoring.di.windApplication
import com.tetiana.wind_speed_monitoring.domain.logic.EnergyAggregator
import com.tetiana.wind_speed_monitoring.domain.model.FarmReport
import com.tetiana.wind_speed_monitoring.domain.model.FleetStatistics
import com.tetiana.wind_speed_monitoring.domain.model.TurbineFilter
import com.tetiana.wind_speed_monitoring.domain.model.TurbineSort
import com.tetiana.wind_speed_monitoring.domain.validation.EnergyValidator
import com.tetiana.wind_speed_monitoring.domain.validation.toInputString
import com.tetiana.wind_speed_monitoring.domain.validation.toNumberOrNull
import com.tetiana.wind_speed_monitoring.domain.model.UserSettings
import com.tetiana.wind_speed_monitoring.viewmodel.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Усі дані екрана турбін, що приходять одним Success
data class TurbinesContent(
    val farms: List<WindFarmEntity>,
    val turbines: List<TurbineSummary>,
    val reports: List<FarmReport>,
    val fleet: FleetStatistics
)

//
//  ViewModel екрана турбін (шар ViewModel в MVVM).
//  - Не знає про Compose: публікує лише StateFlow / Flow.
//  - Об'єднує (combine) кілька потоків Repository в один UiState.
//  - flatMapLatest: при зміні фільтра попередній SQL-запит скасовується
//    і запускається новий - фільтрація/сортування реактивні.
//  - stateIn(WhileSubscribed(5000)): запити до БД працюють, лише поки
//    екран видимий (+5 с на поворот екрана без перезапуску)
//
@OptIn(ExperimentalCoroutinesApi::class)
class TurbinesViewModel(private val repository: WindDataRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TurbineFilter())
    val filter: StateFlow<TurbineFilter> = _filter.asStateFlow()

    // Збільшення лічильника перезапускає потік даних (кнопка "Повторити")
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<TurbinesContent>> = retryTrigger
        .flatMapLatest {
            combine(
                repository.observeFarms(),
                _filter.flatMapLatest { repository.observeTurbineSummaries(it) },
                repository.observeFarmReports()
            ) { farms, turbines, reports ->
                TurbinesContent(farms, turbines, reports, EnergyAggregator.buildFleetStatistics(reports)).asSuccess()
            }
                .onStart { emit(UiState.Loading) }
                .catch { e -> emit(UiState.Error(e.message ?: "Не вдалося завантажити дані")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // Обрана турбіна (для двопанельного режиму на планшеті)
    private val _selectedTurbineId = MutableStateFlow<Long?>(null)
    val selectedTurbineId: StateFlow<Long?> = _selectedTurbineId.asStateFlow()

    // Стан діалогу форми (null - діалог закритий)
    private val _form = MutableStateFlow<FormState?>(null)
    val form: StateFlow<FormState?> = _form.asStateFlow()

    // Турбіна, що очікує підтвердження видалення
    private val _pendingDelete = MutableStateFlow<TurbineSummary?>(null)
    val pendingDelete: StateFlow<TurbineSummary?> = _pendingDelete.asStateFlow()

    // Разові події для Snackbar. Channel - щоб повідомлення не показалось
    // повторно після повороту екрана (на відміну від StateFlow)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty().onFailure { e -> _events.trySend(e.message ?: "Помилка ініціалізації БД") }
        }
    }

    // ---------- Фільтрація та сортування ----------

    fun onSearchChange(query: String) = _filter.update { it.copy(query = query) }
    fun onFarmFilterChange(farmId: Long?) = _filter.update { it.copy(farmId = farmId) }
    fun onSortChange(sort: TurbineSort) = _filter.update { it.copy(sort = sort) }
    fun retry() = retryTrigger.update { it + 1 }
    fun selectTurbine(id: Long?) { _selectedTurbineId.value = id }

    // ---------- Форма додавання / редагування ----------

    fun openAddForm() {
        val farms = currentFarms()
        // Інтеграція зі збереженими налаштуваннями користувача:
        // нова турбіна отримує його радіус і ККД за замовчуванням
        val defaults = SessionManager.currentUser?.settings ?: UserSettings()
        _form.value = buildTurbineForm(
            title = "Нова турбіна",
            farms = farms,
            farmId = _filter.value.farmId ?: farms.firstOrNull()?.id,
            radius = defaults.defaultBladeRadius,
            efficiency = defaults.defaultEfficiency,
            confirmText = "Додати"
        )
    }

    fun openEditForm(turbine: TurbineSummary) {
        _form.value = buildTurbineForm(
            title = "Редагувати ${turbine.name}",
            farms = currentFarms(),
            farmId = turbine.farmId,
            name = turbine.name,
            radius = turbine.bladeRadius.toInputString(),
            efficiency = turbine.efficiency.toInputString(),
            ratedPower = turbine.ratedPowerKw.toInputString(),
            editingId = turbine.id
        )
    }

    fun onFormValueChange(key: String, value: String) = _form.update { it?.withValue(key, value) }
    fun onFormOptionSelect(key: String, optionId: Long) = _form.update { it?.withOption(key, optionId) }

    fun dismissForm() {
        if (_form.value?.isSaving != true) _form.value = null
    }

    // Валідація -> стан збереження -> запис у Room -> закриття форми або показ помилки
    fun saveForm() {
        val state = _form.value ?: return
        val farmId = state.selectedOption(EnergyValidator.FIELD_FARM)
        val errors = EnergyValidator.validateTurbine(
            farmId = farmId,
            name = state.value(EnergyValidator.FIELD_NAME),
            radius = state.value(EnergyValidator.FIELD_RADIUS),
            efficiency = state.value(EnergyValidator.FIELD_EFFICIENCY),
            ratedPower = state.value(EnergyValidator.FIELD_RATED_POWER)
        )
        if (errors.isNotEmpty() || farmId == null) {
            _form.value = state.withErrors(errors)
            return
        }

        val name = state.value(EnergyValidator.FIELD_NAME)
        val radius = state.value(EnergyValidator.FIELD_RADIUS).toNumberOrNull() ?: return
        val efficiency = state.value(EnergyValidator.FIELD_EFFICIENCY).toNumberOrNull() ?: return
        val ratedPower = state.value(EnergyValidator.FIELD_RATED_POWER).toNumberOrNull() ?: return

        _form.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val editingId = state.editingId
            val result: Result<String> = if (editingId == null) {
                repository.addTurbine(farmId, name, radius, efficiency, ratedPower)
                    .map { "Турбіну «${name.trim()}» додано" }
            } else {
                repository.updateTurbine(editingId, farmId, name, radius, efficiency, ratedPower)
                    .map { count -> "Зміни збережено, перераховано вимірювань: $count" }
            }
            result
                .onSuccess { message ->
                    _form.value = null
                    _events.trySend(message)
                }
                .onFailure { e ->
                    _form.value = state.copy(isSaving = false)
                    _events.trySend(e.message ?: "Не вдалося зберегти")
                }
        }
    }

    // ---------- Видалення ----------

    fun requestDelete(turbine: TurbineSummary) { _pendingDelete.value = turbine }
    fun cancelDelete() { _pendingDelete.value = null }

    fun confirmDelete() {
        val turbine = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch {
            repository.deleteTurbine(turbine.id)
                .onSuccess {
                    if (_selectedTurbineId.value == turbine.id) _selectedTurbineId.value = null
                    _events.trySend("«${turbine.name}» видалено разом з вимірюваннями")
                }
                .onFailure { e -> _events.trySend(e.message ?: "Не вдалося видалити") }
        }
    }

    // ---------- Допоміжне ----------

    private fun currentFarms(): List<WindFarmEntity> =
        (uiState.value as? UiState.Success)?.data?.farms.orEmpty()

    private fun buildTurbineForm(
        title: String,
        farms: List<WindFarmEntity>,
        farmId: Long?,
        name: String = "",
        radius: String = "40",
        efficiency: String = "35",
        ratedPower: String = "2500",
        editingId: Long? = null,
        confirmText: String = "Зберегти"
    ) = FormState(
        title = title,
        confirmText = confirmText,
        editingId = editingId,
        fields = listOf(
            FormField(
                key = EnergyValidator.FIELD_FARM, label = "Станція",
                options = farms.map { FormOption(it.id, it.name) }, selectedOptionId = farmId
            ),
            FormField(EnergyValidator.FIELD_NAME, "Назва турбіни", name, hint = "Наприклад: ВТ-04"),
            FormField(EnergyValidator.FIELD_RADIUS, "Радіус лопатей (м)", radius, hint = "0–150 м", type = FieldType.Number),
            FormField(EnergyValidator.FIELD_EFFICIENCY, "ККД (%)", efficiency, hint = "Не більше 59 % (межа Беца)", type = FieldType.Number),
            FormField(EnergyValidator.FIELD_RATED_POWER, "Номінальна потужність (кВт)", ratedPower, hint = "0–15000 кВт", type = FieldType.Number)
        )
    )

    companion object {
        // Фабрика: ViewModel отримує Repository з AppContainer
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TurbinesViewModel(windApplication().container.windRepository) }
        }
    }
}