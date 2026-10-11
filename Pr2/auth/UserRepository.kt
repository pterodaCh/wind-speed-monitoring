package com.tetiana.wind_speed_monitoring.auth

import com.tetiana.wind_speed_monitoring.domain.model.User
import com.tetiana.wind_speed_monitoring.domain.model.UserSettings

//
//  Сховище користувачів (у пам'яті, на час роботи застосунку).
//  Зберігає стан користувача, email та налаштування.
//  Логін і email перевіряються на унікальність без урахування регістру
//
object UserRepository {
    // Початково додаємо одного "адміна"
    private val users = mutableMapOf(
        User.ADMIN_LOGIN to User(login = User.ADMIN_LOGIN, email = "admin@ves.ua", password = "123456")
    )

    fun checkCredentials(identifier: String, pass: String): Boolean {
        return findByLoginOrEmail(identifier)?.password == pass
    }

    fun userExists(login: String): Boolean {
        return users.keys.any { it.equals(login.trim(), ignoreCase = true) }
    }

    fun emailExists(email: String): Boolean {
        return users.values.any { it.email.equals(email.trim(), ignoreCase = true) }
    }

    // Вхід можливий як за логіном, так і за email
    fun findByLoginOrEmail(identifier: String): User? {
        val id = identifier.trim()
        return users[id] ?: users.values.firstOrNull { it.email.equals(id, ignoreCase = true) }
    }

    fun register(login: String, email: String, pass: String) {
        users[login] = User(login = login, email = email.trim().lowercase(), password = pass)
    }

    fun getUser(login: String): User? = users[login]

    // Фіксуємо вхід: лічильник входів і час останнього входу
    fun recordLogin(login: String) {
        val user = users[login] ?: return
        users[login] = user.copy(
            loginCount = user.loginCount + 1,
            lastLoginAt = System.currentTimeMillis()
        )
    }

    // Зберігаємо налаштування конкретного користувача
    fun saveSettings(login: String, settings: UserSettings) {
        val user = users[login] ?: return
        users[login] = user.copy(settings = settings)
    }
}