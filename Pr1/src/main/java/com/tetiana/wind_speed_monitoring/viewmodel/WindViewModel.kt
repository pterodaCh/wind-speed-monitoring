package com.tetiana.wind_speed_monitoring.viewmodel

import androidx.lifecycle.ViewModel
import com.tetiana.wind_speed_monitoring.model.SystemStatus
import com.tetiana.wind_speed_monitoring.model.WindTurbineUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI

class WindViewModel : ViewModel() {

    private companion object {
        const val CALM_THRESHOLD = 3     // м/с
        const val STORM_THRESHOLD = 25   // м/с
        const val AIR_DENSITY = 1.225    // Щільність повітря (кг/м3)
    }

    private val _uiState = MutableStateFlow(WindTurbineUiState())
    val uiState: StateFlow<WindTurbineUiState> = _uiState

    // Функції для оновлення кожного окремого поля
    fun onWindSpeedChanged(newValue: String) {
        _uiState.update { it.copy(windSpeedInput = newValue, windSpeedError = null) }
    }

    fun onBladeRadiusChanged(newValue: String) {
        _uiState.update { it.copy(bladeRadiusInput = newValue, bladeRadiusError = null) }
    }

    fun onEfficiencyChanged(newValue: String) {
        _uiState.update { it.copy(efficiencyInput = newValue, efficiencyError = null) }
    }

    // Головна функція обчислення та валідації
    fun calculateGeneration() {
        val state = _uiState.value
        val windSpeed = state.windSpeedInput.toIntOrNull()
        val radius = state.bladeRadiusInput.toDoubleOrNull()
        val efficiency = state.efficiencyInput.toDoubleOrNull()

        var hasError = false
        var wError: String? = null
        var rError: String? = null
        var eError: String? = null

        // Валідація всіх трьох параметрів
        if (windSpeed == null || windSpeed < 0) {
            wError = "Введіть додатне ціле число"
            hasError = true
        }
        if (radius == null || radius <= 0) {
            rError = "Введіть коректний радіус (напр. 40.5)"
            hasError = true
        }
        if (efficiency == null || efficiency <= 0 || efficiency > 100) {
            eError = "Введіть ККД від 1 до 100"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(windSpeedError = wError, bladeRadiusError = rError, efficiencyError = eError)
            }
            return
        }

        // Розрахунлок
        if (windSpeed != null && radius != null && efficiency != null) {
            val powerKw = calculatePowerKw(windSpeed, radius, efficiency)
            _uiState.update {
                it.copy(
                    windSpeed = windSpeed,
                    power = powerKw,
                    status = resolveStatus(windSpeed),
                    windSpeedError = null,
                    bladeRadiusError = null,
                    efficiencyError = null
                )
            }
        }
    }

    // P = 0.5 * ρ * A * V3 * Cp
    private fun calculatePowerKw(v: Int, r: Double, eff: Double): Int = when {
        v < CALM_THRESHOLD || v > STORM_THRESHOLD -> 0
        else -> {
            val area = PI * r * r // Площа кола, яку ометають лопаті
            val efficiencyFactor = eff / 100.0
            val powerWatts = 0.5 * AIR_DENSITY * area * (v * v * v) * efficiencyFactor
            (powerWatts / 1000).toInt() // Конвертація Ватів у Кіловати
        }
    }

    private fun resolveStatus(speed: Int): SystemStatus = when {
        speed < CALM_THRESHOLD -> SystemStatus.Calm
        speed > STORM_THRESHOLD -> SystemStatus.StormWarning
        else -> SystemStatus.Generating
    }
}