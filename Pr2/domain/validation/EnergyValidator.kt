package com.tetiana.wind_speed_monitoring.domain.validation

//
//  Валідація енергетичних параметрів перед записом у БД.
//  Повертає Map "ключ поля → текст помилки", тому універсальна форма
//  показує кожну помилку саме під своїм полем
//
object EnergyValidator {
    const val FIELD_FARM = "farm"
    const val FIELD_NAME = "name"
    const val FIELD_RADIUS = "radius"
    const val FIELD_EFFICIENCY = "efficiency"
    const val FIELD_RATED_POWER = "ratedPower"
    const val FIELD_WIND = "wind"

    private const val MAX_NAME_LENGTH = 30
    private const val MAX_RADIUS_M = 150.0
    private const val BETZ_LIMIT_PERCENT = 59.0 // межа Беца
    private const val MAX_RATED_POWER_KW = 15_000.0
    private const val MAX_WIND_SPEED = 60.0

    fun validateTurbine(
        farmId: Long?,
        name: String,
        radius: String,
        efficiency: String,
        ratedPower: String
    ): Map<String, String> = buildMap {
        if (farmId == null) put(FIELD_FARM, "Оберіть станцію")
        when {
            name.isBlank() -> put(FIELD_NAME, "Назва не може бути порожньою")
            name.trim().length > MAX_NAME_LENGTH -> put(FIELD_NAME, "Не довше $MAX_NAME_LENGTH символів")
        }
        positiveNumberError(radius, MAX_RADIUS_M, "м")?.let { put(FIELD_RADIUS, it) }
        positiveNumberError(efficiency, BETZ_LIMIT_PERCENT, "% (межа Беца)")?.let { put(FIELD_EFFICIENCY, it) }
        positiveNumberError(ratedPower, MAX_RATED_POWER_KW, "кВт")?.let { put(FIELD_RATED_POWER, it) }
    }

    fun validateMeasurement(windSpeed: String): Map<String, String> {
        val value = windSpeed.toNumberOrNull()
        val error = when {
            windSpeed.isBlank() -> "Поле не може бути порожнім"
            value == null -> "Введіть число, наприклад 12.5"
            value < 0 -> "Швидкість не може бути від'ємною"
            value > MAX_WIND_SPEED -> "Не більше ${MAX_WIND_SPEED.toInt()} м/с - перевірте сенсор"
            else -> null
        }
        return if (error == null) emptyMap() else mapOf(FIELD_WIND to error)
    }

    private fun positiveNumberError(raw: String, max: Double, unit: String): String? {
        val value = raw.toNumberOrNull()
        return when {
            raw.isBlank() -> "Поле не може бути порожнім"
            value == null -> "Введіть число"
            value <= 0 -> "Значення має бути більше 0"
            value > max -> "Не більше ${max.toInputString()} $unit"
            else -> null
        }
    }
}

// Приймаємо і крапку, і кому як десятковий роздільник
fun String.toNumberOrNull(): Double? = replace(',', '.').trim().toDoubleOrNull()

// 45.0 -> "45", 12.5 -> "12.5" - для заповнення полів форми
fun Double.toInputString(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()