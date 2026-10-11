package com.tetiana.wind_speed_monitoring.auth

import com.tetiana.wind_speed_monitoring.domain.model.PasswordStrength

//
//  Одне правило складності пароля.
//  isRequired = false - правило лише підвищує надійність, але не обов'язкове
//
data class PasswordRule(
    val description: String,
    val isRequired: Boolean = true,
    val check: (String) -> Boolean
)

//
//  Політика паролів - бізнес-логіка перевірки складності.
//  Щоб додати нову вимогу, достатньо додати PasswordRule у список rules:
//  вона автоматично з'явиться і в перевірці, і в чек-листі на екрані
//
object PasswordPolicy {

    const val MIN_LENGTH = 8

    val rules = listOf(
        PasswordRule("Щонайменше $MIN_LENGTH символів") { it.length >= MIN_LENGTH },
        PasswordRule("Велика літера (A–Z)") { p -> p.any { it.isUpperCase() } },
        PasswordRule("Мала літера (a–z)") { p -> p.any { it.isLowerCase() } },
        PasswordRule("Цифра (0–9)") { p -> p.any { it.isDigit() } },
        PasswordRule("Спецсимвол (!@#\$…) - бажано", isRequired = false) { p -> p.any { !it.isLetterOrDigit() } }
    )

    // Найпоширеніші паролі, які легко підібрати
    private val COMMON_PASSWORDS = setOf(
        "password", "password1", "12345678", "123456789",
        "qwerty123", "qwertyuiop", "admin123", "11111111"
    )

    // Повна перевірка пароля. Повертає текст першої помилки або null
    fun validate(password: String, login: String = ""): String? {
        val unmetRule = rules.firstOrNull { it.isRequired && !it.check(password) }
        return when {
            password.lowercase() in COMMON_PASSWORDS -> "Пароль занадто поширений - оберіть інший"
            login.length >= AuthValidator.MIN_LOGIN_LENGTH && password.contains(login, ignoreCase = true) ->
                "Пароль не повинен містити логін"
            unmetRule != null -> "Пароль має містити: ${unmetRule.description.replaceFirstChar { it.lowercase() }}"
            else -> null
        }
    }

    // Рівень надійності за кількістю виконаних правил
    fun strengthOf(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.Empty
        val satisfied = rules.count { it.check(password) }
        return when {
            satisfied <= 2 -> PasswordStrength.Weak
            satisfied <= 4 -> PasswordStrength.Medium
            else -> PasswordStrength.Strong
        }
    }

    // Стан кожного правила для чек-листа: (опис, виконано?)
    fun requirementStates(password: String): List<Pair<String, Boolean>> =
        rules.map { it.description to it.check(password) }
}