package com.tetiana.wind_speed_monitoring.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementSource
import com.tetiana.wind_speed_monitoring.domain.model.MeasurementStatus

//
//  Схема БД (3 таблиці, зв'язки "один-до-багатьох"):
//
//   wind_farms 1 ──< turbines 1 ──< measurements
//
//  ForeignKey + CASCADE: видалення станції видаляє її турбіни,
//  а видалення турбіни - усі її вимірювання (цілісність даних гарантує SQLite).
//  Індекси на зовнішніх ключах і timestamp пришвидшують JOIN, фільтрацію та сортування
//

// Вітрова електростанція
@Entity(
    tableName = "wind_farms",
    indices = [Index(value = ["name"], unique = true)]
)
data class WindFarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val region: String
)

// Турбіна. Унікальність назви - в межах однієї станції
@Entity(
    tableName = "turbines",
    foreignKeys = [
        ForeignKey(
            entity = WindFarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("farmId"), Index(value = ["farmId", "name"], unique = true)]
)
data class TurbineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val name: String,
    val bladeRadius: Double,   // м
    val efficiency: Double,    // %
    val ratedPowerKw: Double,  // номінальна потужність, кВт
    val createdAt: Long = System.currentTimeMillis()
)

// Вимірювання. powerKw і status - обчислювані поля:
// їх рахує PowerCalculator у Repository перед записом, а не вводить користувач
@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = TurbineEntity::class,
            parentColumns = ["id"],
            childColumns = ["turbineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("turbineId"), Index("timestamp")]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val turbineId: Long,
    val windSpeed: Double,         // м/с
    val powerKw: Double,           // обчислюється
    val status: MeasurementStatus, // обчислюється
    val source: MeasurementSource,
    val timestamp: Long
)