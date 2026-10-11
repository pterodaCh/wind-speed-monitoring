package com.tetiana.wind_speed_monitoring.auth

//
//  Помилки форм окремо для кожного поля -
//  щоб показувати повідомлення саме під тим полем, де помилка
//

// Помилки форми входу
data class LoginErrors(
    val identifier: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean
        get() = identifier != null || password != null
}

// Помилки форми реєстрації
data class RegistrationErrors(
    val login: String? = null,
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
) {
    val hasErrors: Boolean
        get() = listOfNotNull(login, email, password, confirmPassword).isNotEmpty()
}