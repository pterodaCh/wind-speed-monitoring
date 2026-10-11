package com.tetiana.wind_speed_monitoring.auth

import kotlinx.coroutines.delay
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

//
//  Сервіс обробки даних після натискання кнопки.
//  suspend-функції з delay імітують мережевий запит - саме під час
//  цієї паузи UI показує стан завантаження.
//  У реальному проєкті тут був би виклик API / бази даних
//
object AuthService {

    private const val PROCESSING_DELAY_MS = 1500L

    // Вхід за логіном або email
    suspend fun login(identifier: String, password: String): AuthResult {
        delay(PROCESSING_DELAY_MS)
        val user = UserRepository.findByLoginOrEmail(identifier)
        return if (user != null && user.password == password) {
            SessionManager.login(user.login)
            AuthResult.Success
        } else {
            AuthResult.Error("Невірний логін/email або пароль")
        }
    }

    // Реєстрація. Унікальність перевіряється повторно, бо за час
    // "запиту" логін чи email міг зайняти інший користувач
    suspend fun register(login: String, email: String, password: String): AuthResult {
        delay(PROCESSING_DELAY_MS)
        return when {
            UserRepository.userExists(login) -> AuthResult.Error("Логін щойно зайняли - оберіть інший")
            UserRepository.emailExists(email) -> AuthResult.Error("Цей email щойно зареєстрували")
            else -> {
                UserRepository.register(login, email, password)
                SessionManager.login(login)
                AuthResult.Success
            }
        }
    }
}