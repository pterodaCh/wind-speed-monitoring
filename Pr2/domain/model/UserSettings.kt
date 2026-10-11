package com.tetiana.wind_speed_monitoring.domain.model

//
//  Збережені налаштування користувача.
//  Значення полів зберігаються рядками, бо саме так їх вводить користувач
//  і так їх приймає WindViewModel (onBladeRadiusChanged / onEfficiencyChanged)
//
data class UserSettings(
    val defaultBladeRadius: String = "40",
    val defaultEfficiency: String = "35",
    val turbineAnimationEnabled: Boolean = true,
    val autoFillParameters: Boolean = true
)

//
//  Валідація налаштувань винесена з UI в окремий об'єкт
//
object SettingsValidator {
    private const val MAX_BLADE_RADIUS_M = 150.0
    // Межа Беца: теоретично вітрова турбіна не може перетворити більше ~59 % енергії вітру
    private const val BETZ_LIMIT_PERCENT = 59.0

    fun validate(bladeRadius: String, efficiency: String): String? {
        val radius = bladeRadius.replace(',', '.').toDoubleOrNull()
        val eff = efficiency.replace(',', '.').toDoubleOrNull()
        return when {
            radius == null -> "Радіус лопатей має бути числом"
            radius <= 0 || radius > MAX_BLADE_RADIUS_M -> "Радіус лопатей: від 0 до ${MAX_BLADE_RADIUS_M.toInt()} м"
            eff == null -> "ККД має бути числом"
            eff <= 0 -> "ККД має бути більшим за 0 %"
            eff > BETZ_LIMIT_PERCENT -> "ККД не може перевищувати межу Беца (${BETZ_LIMIT_PERCENT.toInt()} %)"
            else -> null
        }
    }
}
