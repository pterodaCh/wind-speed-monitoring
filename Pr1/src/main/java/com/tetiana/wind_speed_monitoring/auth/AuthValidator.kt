package com.tetiana.wind_speed_monitoring.auth

object AuthValidator {

    fun validateLogin(login: String, pass: String): String? {
        return when {
            login.isBlank() -> "Логін не може бути порожнім"
            pass.isBlank() -> "Пароль не може бути порожнім"
            // Звернення до сховища для перевірки
            !UserRepository.checkCredentials(login, pass) -> "Невірний логін або пароль"
            else -> null
        }
    }

    fun validateRegistration(login: String, pass: String, confirmPass: String): String? {
        return when {
            login.isBlank() -> "Логін не може бути порожнім"
            login.length < 3 -> "Логін має містити щонайменше 3 символи"
            // Перевірка логіну чм вже не зайнятий кимось іншим
            UserRepository.userExists(login) -> "Користувач з таким логіном вже існує"
            pass.isBlank() -> "Пароль не може бути порожнім"
            pass.length < 6 -> "Пароль має містити щонайменше 6 символів"
            pass != confirmPass -> "Паролі не збігаються"
            else -> null
        }
    }
}