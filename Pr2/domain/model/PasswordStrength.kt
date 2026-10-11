package com.tetiana.wind_speed_monitoring.domain.model
//
//  Рівень надійності пароля
//  progress заповнення шкали (0..1), label підпис під шкалою
//
enum class PasswordStrength(val label: String, val progress: Float) {
    Empty("", 0f),
    Weak("Слабкий", 0.33f),
    Medium("Середній", 0.66f),
    Strong("Надійний", 1f)
}