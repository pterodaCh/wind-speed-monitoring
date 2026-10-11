package com.tetiana.wind_speed_monitoring.data.remote

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.random.Random

// Показання анемометра
data class SensorReading(val turbineId: Long, val windSpeed: Double, val timestamp: Long)

//
//  Друге джерело даних - "віддалений" анемометр (імітація мережевого API).
//  delay імітує затримку мережі, а випадкова IOException - збій зв'язку.
//  Це дозволяє показати в UI стани завантаження, успіху та помилки
//
class SensorDataSource(private val random: Random = Random.Default) {

    suspend fun fetchReading(turbineId: Long): SensorReading {
        delay(NETWORK_DELAY_MS)
        if (random.nextInt(100) < FAILURE_RATE_PERCENT) {
            throw IOException("анемометр турбіни #$turbineId не відповідає")
        }
        val windSpeed = (random.nextDouble(1.0, 28.0) * 10).roundToInt() / 10.0
        return SensorReading(turbineId, windSpeed, System.currentTimeMillis())
    }

    private companion object {
        const val NETWORK_DELAY_MS = 1200L
        const val FAILURE_RATE_PERCENT = 20
    }
}