package com.tetiana.wind_speed_monitoring.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.domain.model.PasswordStrength


//    Індикатор надійності пароля: кольорова шкала + підпис + чек-лист вимог.
//    Оновлюється на кожен введений символ, бо отримує дані,
//    обчислені з поточного State пароля.
//    Stateless: рівень і стан вимог передаються ззовні
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    requirements: List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier
) {
    // Колір шкали залежить від рівня надійності
    val color = when (strength) {
        PasswordStrength.Weak -> MaterialTheme.colorScheme.error
        PasswordStrength.Medium -> MaterialTheme.colorScheme.tertiary
        PasswordStrength.Strong -> MaterialTheme.colorScheme.primary
        PasswordStrength.Empty -> MaterialTheme.colorScheme.outline
    }
    // Плавне заповнення шкали
    val animatedProgress by animateFloatAsState(
        targetValue = strength.progress,
        label = "PasswordStrengthAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    ) {
        if (strength != PasswordStrength.Empty) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strength.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        requirements.forEach { (description, satisfied) ->
            PasswordRequirementRow(description = description, satisfied = satisfied)
        }
    }
}


//    Один рядок чек-листа вимог до пароля: іконка ✓ / ○ + опис
@Composable
fun PasswordRequirementRow(description: String, satisfied: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (satisfied) "Виконано" else "Не виконано",
            modifier = Modifier.size(16.dp),
            tint = if (satisfied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = if (satisfied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}