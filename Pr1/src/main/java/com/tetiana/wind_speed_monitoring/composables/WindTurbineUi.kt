package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.R


//    швидкість обертання обернено пропорційна
//    швидкості вітру
//    але обмежена знизу [MIN_DURATION_MS], щоб анімація не "миготіла".
//    Турбіна стоїть (duration = 0), якщо вітру немає або оголошено шторм
@Composable
fun WindmillTurbine(windSpeed: Int) {
    val duration = rotationDurationMs(windSpeed)

    val infiniteTransition = rememberInfiniteTransition(label = "turbine_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (duration > 0) duration else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Image(
        painter = painterResource(id = R.drawable.propeller),
        contentDescription = "Лопаті вітрогенератора",
        modifier = Modifier.size(120.dp).rotate(if (duration > 0) rotation else 0f)
    )
}

private const val MIN_DURATION_MS = 300
private const val BASE_DURATION_MS = 2500
private const val SPEED_TO_MS_FACTOR = 60

private fun rotationDurationMs(windSpeed: Int): Int = when {
    windSpeed < 3 || windSpeed > 25 -> 0
    else -> maxOf(MIN_DURATION_MS, BASE_DURATION_MS - windSpeed * SPEED_TO_MS_FACTOR)
}