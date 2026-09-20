package com.tetiana.wind_speed_monitoring.model

//
//  Незмінний UI екрана моніторингу
//  ViewModel створює новий екземпляр при кожній зміні даних (copy()),
//  а Compose перемальовує composable, що читають саме
//  змінені поля
//
data class WindTurbineUiState(
    // Поля введення
    val windSpeedInput: String = "",
    val bladeRadiusInput: String = "",
    val efficiencyInput: String = "",

    // Результати
    val windSpeed: Int = 0,
    val power: Int = 0, // кВт
    val status: SystemStatus = SystemStatus.Idle,

    // Помилки валідації
    val windSpeedError: String? = null,
    val bladeRadiusError: String? = null,
    val efficiencyError: String? = null
)