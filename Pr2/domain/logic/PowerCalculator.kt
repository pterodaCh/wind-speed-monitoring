package com.tetiana.wind_speed_monitoring.domain.logic

import com.tetiana.wind_speed_monitoring.data.local.relation.FarmWithTurbines
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.domain.model.*
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

//
//  Розрахунок потужності вітрової турбіни:
//      P = ½ · ρ · A · v³ · η,   де A = π · r²
//  Турбіна працює лише в діапазоні [CUT_IN; CUT_OUT]:
//  нижче - штиль, вище - аварійне гальмування (шторм).
//  Потужність обмежується номінальною потужністю генератора
//
object PowerCalculator {
    private const val AIR_DENSITY = 1.225 // кг/м³ на рівні моря
    const val CUT_IN_SPEED = 3.0          // м/с
    const val CUT_OUT_SPEED = 25.0        // м/с

    fun calculate(
        windSpeed: Double,
        bladeRadius: Double,
        efficiencyPercent: Double,
        ratedPowerKw: Double
    ): PowerResult = when {
        windSpeed < CUT_IN_SPEED -> PowerResult(0.0, MeasurementStatus.CALM)
        windSpeed > CUT_OUT_SPEED -> PowerResult(0.0, MeasurementStatus.STORM)
        else -> {
            val sweptArea = PI * bladeRadius.pow(2)
            val powerKw = 0.5 * AIR_DENSITY * sweptArea * windSpeed.pow(3) * (efficiencyPercent / 100) / 1000
            PowerResult(min(powerKw, ratedPowerKw).roundTo1(), MeasurementStatus.GENERATING)
        }
    }

    private fun Double.roundTo1(): Double = (this * 10).roundToInt() / 10.0
}

//
//  Агрегація енергетичних даних (виконується у фоновому потоці Repository).
//  Припущення: кожне вимірювання відповідає 1 годині роботи,
//  тому енергія (кВт·год) = сума потужностей (кВт)
//
object EnergyAggregator {

    // Звіт по станції зі вкладеного зв'язку станція → турбіни → вимірювання
    fun buildFarmReport(farm: FarmWithTurbines): FarmReport {
        val allMeasurements = farm.turbines.flatMap { it.measurements }
        val bestTurbine = farm.turbines
            .filter { it.measurements.isNotEmpty() }
            .maxByOrNull { turbine -> turbine.measurements.sumOf { it.powerKw } }

        return FarmReport(
            farmId = farm.farm.id,
            farmName = farm.farm.name,
            region = farm.farm.region,
            turbineCount = farm.turbines.size,
            measurementCount = allMeasurements.size,
            energyKwh = allMeasurements.sumOf { it.powerKw },
            avgWindSpeed = allMeasurements.map { it.windSpeed }.averageOrZero(),
            bestTurbineName = bestTurbine?.turbine?.name
        )
    }

    // Загальна статистика. Середній вітер - зважений за кількістю вимірювань
    fun buildFleetStatistics(reports: List<FarmReport>): FleetStatistics {
        val measurements = reports.sumOf { it.measurementCount }
        val weightedWind = if (measurements == 0) 0.0
        else reports.sumOf { it.avgWindSpeed * it.measurementCount } / measurements
        return FleetStatistics(
            turbineCount = reports.sumOf { it.turbineCount },
            measurementCount = measurements,
            totalEnergyKwh = reports.sumOf { it.energyKwh },
            avgWindSpeed = weightedWind
        )
    }

    // Статистика по відфільтрованому журналу однієї турбіни
    fun statsOf(measurements: List<MeasurementEntity>): MeasurementStats {
        val generating = measurements.count { it.status == MeasurementStatus.GENERATING }
        return MeasurementStats(
            count = measurements.size,
            avgWindSpeed = measurements.map { it.windSpeed }.averageOrZero(),
            maxPowerKw = measurements.maxOfOrNull { it.powerKw } ?: 0.0,
            energyKwh = measurements.sumOf { it.powerKw },
            generatingSharePercent = if (measurements.isEmpty()) 0.0 else generating * 100.0 / measurements.size
        )
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}