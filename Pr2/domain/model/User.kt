package com.tetiana.wind_speed_monitoring.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//
//  Користувач системи моніторингу.
//
data class User(
    val login: String,
    val email: String = "",
    val password: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val loginCount: Int = 0,
    val lastLoginAt: Long? = null,
    val settings: UserSettings = UserSettings()
) {
    // Роль визначається логіном для відображення стану користувача в меню
    val role: String
        get() = if (login == ADMIN_LOGIN) "Адміністратор" else "Оператор ВЕС"

    companion object {
        const val ADMIN_LOGIN = "admin"
    }
}

// Форматування часу для UI (дд.мм.рррр гг:хх)
fun Long.toDisplayDate(): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(this))