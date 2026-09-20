package com.tetiana.wind_speed_monitoring.auth

object UserRepository {
    // Початково додаємо одного "адміна"
    private val users = mutableMapOf(
        "admin" to "123456"
    )

    fun checkCredentials(login: String, pass: String): Boolean {
        return users[login] == pass
    }

    fun userExists(login: String): Boolean {
        return users.containsKey(login)
    }

    fun register(login: String, pass: String) {
        users[login] = pass
    }
}