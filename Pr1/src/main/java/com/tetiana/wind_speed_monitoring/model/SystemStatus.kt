package com.tetiana.wind_speed_monitoring.model

//
//  Стани системи моніторингу ВЕС
//  Sealed-ієрархія замість магічних значень
//
sealed class SystemStatus(val label: String) {
    data object Idle : SystemStatus("Очікування даних з анемометра")
    data object Calm : SystemStatus("Штиль (Турбіна зупинена)")
    data object StormWarning : SystemStatus("Штормове попередження (Аварійне гальмування)")
    data object Generating : SystemStatus("Нормальна генерація")
}