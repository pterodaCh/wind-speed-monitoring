package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


//Картка з поточними показниками

@Composable
fun WindStatusCard(windSpeed: Int, power: Int, statusLabel: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Поточна швидкість вітру:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$windSpeed м/с", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)

            Spacer(Modifier.height(24.dp))

            Text("Генерація потужності:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$power кВт", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(24.dp))

            Text(statusLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
        }
    }
}