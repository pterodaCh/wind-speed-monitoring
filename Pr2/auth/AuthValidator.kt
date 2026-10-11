package com.tetiana.wind_speed_monitoring.auth

//
//  Бізнес-логіка перевірки даних авторизації/реєстрації.
//  UI нічого не перевіряє сам - лише викликає ці функції.
//  Повертає помилки для кожного поля окремо (LoginErrors / RegistrationErrors)
//
object AuthValidator {

    const val MIN_LOGIN_LENGTH = 3
    const val MAX_LOGIN_LENGTH = 20

    private val LOGIN_REGEX = Regex("^[A-Za-z0-9_]+$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Тимчасові поштові скриньки не приймаємо
    private val DISPOSABLE_EMAIL_DOMAINS = setOf(
        "mailinator.com", "tempmail.com", "10minutemail.com", "yopmail.com"
    )

    // ---- Форма входу: базова перевірка на порожні поля ----

    fun validateLoginForm(identifier: String, pass: String): LoginErrors = LoginErrors(
        identifier = if (identifier.isBlank()) "Введіть логін або email" else null,
        password = if (pass.isBlank()) "Пароль не може бути порожнім" else null
    )

    // ---- Форма реєстрації: комплексна перевірка кожного поля ----

    fun validateRegistrationForm(
        login: String,
        email: String,
        pass: String,
        confirmPass: String
    ): RegistrationErrors = RegistrationErrors(
        login = validateNewLogin(login),
        email = validateEmail(email),
        password = validatePassword(pass, login),
        confirmPassword = validateConfirmPassword(pass, confirmPass)
    )

    // Логін: непорожній, довжина, допустимі символи, унікальність
    fun validateNewLogin(login: String): String? = when {
        login.isBlank() -> "Логін не може бути порожнім"
        login.length < MIN_LOGIN_LENGTH -> "Логін має містити щонайменше $MIN_LOGIN_LENGTH символи"
        login.length > MAX_LOGIN_LENGTH -> "Логін не може бути довшим за $MAX_LOGIN_LENGTH символів"
        !LOGIN_REGEX.matches(login) -> "Лише латинські літери, цифри та _"
        // Перевірка, чи логін вже не зайнятий кимось іншим
        UserRepository.userExists(login) -> "Користувач з таким логіном вже існує"
        else -> null
    }

    // Email: непорожній, формат, не тимчасовий домен, унікальність
    fun validateEmail(email: String): String? {
        val normalized = email.trim().lowercase()
        return when {
            normalized.isEmpty() -> "Email не може бути порожнім"
            !EMAIL_REGEX.matches(normalized) -> "Невірний формат email (приклад: name@mail.com)"
            normalized.substringAfter('@') in DISPOSABLE_EMAIL_DOMAINS -> "Тимчасові поштові скриньки не підтримуються"
            UserRepository.emailExists(normalized) -> "Цей email вже зареєстровано"
            else -> null
        }
    }

    // Пароль: непорожній + правила складності з PasswordPolicy
    fun validatePassword(pass: String, login: String = ""): String? = when {
        pass.isEmpty() -> "Пароль не може бути порожнім"
        else -> PasswordPolicy.validate(pass, login)
    }

    fun validateConfirmPassword(pass: String, confirmPass: String): String? = when {
        confirmPass.isEmpty() -> "Повторіть пароль"
        pass != confirmPass -> "Паролі не збігаються"
        else -> null
    }

    // ---- Живі підказки та помилки під час введення ----

    fun loginHint(login: String): String? = when {
        login.isEmpty() -> "Від $MIN_LOGIN_LENGTH до $MAX_LOGIN_LENGTH символів: латиниця, цифри, _"
        login.length < MIN_LOGIN_LENGTH -> "Ще ${MIN_LOGIN_LENGTH - login.length} симв."
        else -> null
    }

    fun loginLiveError(login: String): String? = when {
        login.isNotEmpty() && !LOGIN_REGEX.matches(login) -> "Лише латинські літери, цифри та _"
        UserRepository.userExists(login) -> "Цей логін вже зайнятий"
        else -> null
    }

    fun emailHint(email: String): String? =
        if (email.isBlank()) "Наприклад: name@mail.com" else null

    // Унікальність перевіряємо одразу, щойно email має коректний формат
    fun emailLiveError(email: String): String? {
        val normalized = email.trim().lowercase()
        return if (EMAIL_REGEX.matches(normalized) && UserRepository.emailExists(normalized))
            "Цей email вже зареєстровано" else null
    }

    fun confirmPasswordLiveError(pass: String, confirmPass: String): String? =
        if (confirmPass.isNotEmpty() && pass != confirmPass) "Паролі поки не збігаються" else null
}