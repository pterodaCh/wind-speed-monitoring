package com.tetiana.wind_speed_monitoring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.data.repository.WindDataRepository
import com.tetiana.wind_speed_monitoring.di.windApplication
import com.tetiana.wind_speed_monitoring.domain.logic.EnergyAggregator
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementFilter
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementSort
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementStats
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementStatus
import com.tetiana.wind_speed_monitoring.domain.validation.EnergyValidator
import com.tetiana.wind_speed_monitoring.domain.validation.toInputString
import com.tetiana.wind_speed_monitoring.domain.validation.toNumberOrNull
import com.tetiana.wind_speed_monitoring.viewmodel.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MeasurementsContent(
    val turbine: TurbineEntity,
    val measurements: List<MeasurementEntity>,
    val stats: MeasurementStats
)

//
//  ViewModel журналу вимірювань однієї турбіни.
//  turbineId передається через фабрику, тому кожна турбіна має власний
//  екземпляр ViewModel (зі своїм фільтром і станом сенсора)
//
@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementsViewModel(
    private val repository: WindDataRepository,
    private val turbineId: Long
) : ViewModel() {

    private val _filter = MutableStateFlow(MeasurementFilter())
    val filter: StateFlow<MeasurementFilter> = _filter.asStateFlow()

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<MeasurementsContent>> = retryTrigger
        .flatMapLatest {
            combine(
                repository.observeTurbine(turbineId),
                _filter.flatMapLatest { repository.observeMeasurements(turbineId, it) }
            ) { turbine, measurements ->
                if (turbine == null) UiState.Error("Турбіну не знайдено - можливо, її видалено")
                else MeasurementsContent(turbine, measurements, EnergyAggregator.statsOf(measurements)).asSuccess()
            }
                .onStart { emit(UiState.Loading) }
                .catch { e -> emit(UiState.Error(e.message ?: "Не вдалося завантажити вимірювання")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // Окремий стан запиту до сенсора (Idle / Running / Success / Error)
    private val _sensorState = MutableStateFlow<OperationState>(OperationState.Idle)
    val sensorState: StateFlow<OperationState> = _sensorState.asStateFlow()

    private val _form = MutableStateFlow<FormState?>(null)
    val form: StateFlow<FormState?> = _form.asStateFlow()
    private var editingMeasurement: MeasurementEntity? = null

    private val _pendingDelete = MutableStateFlow<MeasurementEntity?>(null)
    val pendingDelete: StateFlow<MeasurementEntity?> = _pendingDelete.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    // ---------- Фільтрація та сортування ----------

    fun onStatusFilterChange(status: MeasurementStatus?) = _filter.update { it.copy(status = status) }
    fun onMinWindChange(minWind: Double) = _filter.update { it.copy(minWindSpeed = minWind) }
    fun onSortChange(sort: MeasurementSort) = _filter.update { it.copy(sort = sort) }
    fun retry() = retryTrigger.update { it + 1 }

    // ---------- Сенсор (асинхронно, з кешем у Repository) ----------

    fun fetchFromSensor(forceRefresh: Boolean = false) {
        if (_sensorState.value is OperationState.Running) return
        _sensorState.value = OperationState.Running
        viewModelScope.launch {
            repository.fetchFromSensor(turbineId, forceRefresh)
                .onSuccess { result ->
                    val m = result.measurement
                    _sensorState.value = OperationState.Success(
                        if (result.fromCache) "З кешу: ${m.windSpeed} м/с → ${m.powerKw} кВт. Для нових даних - примусове оновлення"
                        else "Отримано: ${m.windSpeed} м/с → ${m.powerKw} кВт (${m.status.label})"
                    )
                }
                .onFailure { e -> _sensorState.value = OperationState.Error(e.message ?: "Помилка сенсора") }
        }
    }

    fun clearSensorState() { _sensorState.value = OperationState.Idle }

    // ---------- Форма ----------

    fun openAddForm() {
        editingMeasurement = null
        _form.value = buildForm("Нове вимірювання", "", confirmText = "Додати")
    }

    fun openEditForm(measurement: MeasurementEntity) {
        editingMeasurement = measurement
        _form.value = buildForm("Редагувати вимірювання", measurement.windSpeed.toInputString(), measurement.id)
    }

    fun onFormValueChange(key: String, value: String) = _form.update { it?.withValue(key, value) }

    fun dismissForm() {
        if (_form.value?.isSaving != true) _form.value = null
    }

    fun saveForm() {
        val state = _form.value ?: return
        val raw = state.value(EnergyValidator.FIELD_WIND)
        val errors = EnergyValidator.validateMeasurement(raw)
        if (errors.isNotEmpty()) {
            _form.value = state.withErrors(errors)
            return
        }
        val windSpeed = raw.toNumberOrNull() ?: return

        _form.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val editing = editingMeasurement
            val result = if (editing == null) repository.addMeasurement(turbineId, windSpeed)
            else repository.updateMeasurement(editing, windSpeed)

            result
                .onSuccess { m ->
                    _form.value = null
                    _events.trySend("Збережено: ${m.windSpeed} м/с → ${m.powerKw} кВт (${m.status.label})")
                }
                .onFailure { e ->
                    _form.value = state.copy(isSaving = false)
                    _events.trySend(e.message ?: "Не вдалося зберегти")
                }
        }
    }

    // ---------- Видалення ----------

    fun requestDelete(measurement: MeasurementEntity) { _pendingDelete.value = measurement }
    fun cancelDelete() { _pendingDelete.value = null }

    fun confirmDelete() {
        val measurement = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
                .onSuccess { _events.trySend("Вимірювання видалено") }
                .onFailure { e -> _events.trySend(e.message ?: "Не вдалося видалити") }
        }
    }

    private fun buildForm(title: String, wind: String, editingId: Long? = null, confirmText: String = "Зберегти") =
        FormState(
            title = title,
            confirmText = confirmText,
            editingId = editingId,
            fields = listOf(
                FormField(
                    key = EnergyValidator.FIELD_WIND,
                    label = "Швидкість вітру (м/с)",
                    value = wind,
                    hint = "0–60 м/с. Потужність і статус розрахуються автоматично",
                    type = FieldType.Number
                )
            )
        )

    companion object {
        fun factory(turbineId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { MeasurementsViewModel(windApplication().container.windRepository, turbineId) }
        }
    }
}