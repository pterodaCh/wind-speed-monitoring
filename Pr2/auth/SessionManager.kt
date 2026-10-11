package com.tetiana.wind_speed_monitoring.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tetiana.wind_speed_monitoring.domain.model.User
import com.tetiana.wind_speed_monitoring.domain.model.UserSettings

//
//  Поточна сесія користувача.
//  currentUser - це State: будь-який composable, що його читає
//  (меню, налаштування, моніторинг), автоматично перемалюється при зміні
//
object SessionManager {

    var currentUser by mutableStateOf<User?>(null)
        private set

    val isLoggedIn: Boolean
        get() = currentUser != null

    // Викликається після успішної перевірки логіну/пароля або реєстрації
    fun login(login: String) {
        UserRepository.recordLogin(login)
        currentUser = UserRepository.getUser(login)
    }

    // Зберегти нові налаштування і одразу оновити стан сесії
    fun updateSettings(settings: UserSettings) {
        val user = currentUser ?: return
        UserRepository.saveSettings(user.login, settings)
        currentUser = UserRepository.getUser(user.login)
    }

    fun logout() {
        currentUser = null
    }
}
