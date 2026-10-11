package com.tetiana.wind_speed_monitoring.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.tetiana.wind_speed_monitoring.data.local.entity.MeasurementEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.TurbineEntity
import com.tetiana.wind_speed_monitoring.data.local.entity.WindFarmEntity

//
//  Класи зв'язків Room (@Relation).
//  Room сам виконує додаткові запити й "збирає" вкладену структуру.
//  DAO-методи з такими класами позначаються @Transaction, щоб усі
//  підзапити читали узгоджений стан БД
//

// Турбіна разом з усіма вимірюваннями (1 → N)
data class TurbineWithMeasurements(
    @Embedded val turbine: TurbineEntity,
    @Relation(parentColumn = "id", entityColumn = "turbineId")
    val measurements: List<MeasurementEntity>
)

// Вкладений зв'язок: станція → турбіни → вимірювання (1 → N → M)
data class FarmWithTurbines(
    @Embedded val farm: WindFarmEntity,
    @Relation(
        entity = TurbineEntity::class,
        parentColumn = "id",
        entityColumn = "farmId"
    )
    val turbines: List<TurbineWithMeasurements>
)

//
//  Результат агрегатного SQL-запиту (не таблиця!).
//  Обчислювані поля (avgWindSpeed, energyKwh, capacityFactor...) рахує
//  сам SQLite через AVG/MAX/SUM/COUNT: один запит замість завантаження
//  тисяч рядків у пам'ять і підрахунку в Kotlin
//
data class TurbineSummary(
    val id: Long,
    val farmId: Long,
    val name: String,
    val farmName: String,
    val bladeRadius: Double,
    val efficiency: Double,
    val ratedPowerKw: Double,
    val measurementCount: Int,
    val avgWindSpeed: Double,
    val maxPowerKw: Double,
    val energyKwh: Double,
    val capacityFactor: Double,   // КВВП, % - коефіцієнт використання встановленої потужності
    val lastMeasurementAt: Long?
)