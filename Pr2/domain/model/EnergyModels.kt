package com.tetiana.wind_speed_monitoring.domain.model

//
//  Моделі предметної області. Чистий Kotlin без залежностей від Android,
//  тому цю логіку можна перевіряти звичайними unit-тестами
//

// Стан турбіни в момент вимірювання (у БД зберігається як рядок через TypeConverter)
enum class MeasurementStatus(val label: String) {
    GENERATING("Генерація"),
    CALM("Штиль"),
    STORM("Шторм")
}

// Звідки надійшло вимірювання - з ручного введення чи з анемометра
enum class MeasurementSource(val label: String) {
    MANUAL("Вручну"),
    SENSOR("Сенсор")
}

// Варіанти сортування турбін. name передається у SQL (ORDER BY CASE ...)
enum class TurbineSort(val label: String) {
    ENERGY("За енергією"),
    CAPACITY("За КВВП"),
    WIND("За сер. вітром"),
    NAME("За назвою")
}

// Варіанти сортування вимірювань
enum class MeasurementSort(val label: String) {
    DATE_DESC("Спочатку нові"),
    DATE_ASC("Спочатку старі"),
    POWER_DESC("За потужністю"),
    WIND_DESC("За швидкістю вітру")
}

// Фільтр списку турбін: станція (null = усі), пошук за назвою, сортування
data class TurbineFilter(
    val farmId: Long? = null,
    val query: String = "",
    val sort: TurbineSort = TurbineSort.ENERGY
)

// Фільтр журналу вимірювань
data class MeasurementFilter(
    val status: MeasurementStatus? = null,
    val minWindSpeed: Double = 0.0,
    val sort: MeasurementSort = MeasurementSort.DATE_DESC
)

// Результат розрахунку генерації
data class PowerResult(val powerKw: Double, val status: MeasurementStatus)

// Звіт по станції - агрегується в Repository зі зв'язку FarmWithTurbines
data class FarmReport(
    val farmId: Long,
    val farmName: String,
    val region: String,
    val turbineCount: Int,
    val measurementCount: Int,
    val energyKwh: Double,
    val avgWindSpeed: Double,
    val bestTurbineName: String?
)

// Загальна статистика по всіх станціях
data class FleetStatistics(
    val turbineCount: Int,
    val measurementCount: Int,
    val totalEnergyKwh: Double,
    val avgWindSpeed: Double
)

// Статистика по відфільтрованому журналу вимірювань однієї турбіни
data class MeasurementStats(
    val count: Int,
    val avgWindSpeed: Double,
    val maxPowerKw: Double,
    val energyKwh: Double,
    val generatingSharePercent: Double
)