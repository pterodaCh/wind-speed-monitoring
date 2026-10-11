package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.components.InfoCard
import com.tetiana.wind_speed_monitoring.components.InfoRow
import com.tetiana.wind_speed_monitoring.domain.model.User
import com.tetiana.wind_speed_monitoring.domain.model.toDisplayDate


//    Картка зі станом користувача та його збереженими налаштуваннями.
//    Stateless: отримує User ззовні (з SessionManager), тому оновлюється
//    одразу після збереження нових налаштувань.
//    Використовується в головному меню та на екрані налаштувань
@Composable
fun UserStatusCard(user: User?, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        if (user == null) {
            Text(
                "Сесію не знайдено. Увійдіть повторно.",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            InfoRow("Логін", user.login)
            InfoRow("Email", user.email.ifBlank { "-" })
            InfoRow("Роль", user.role)
            InfoRow("Зареєстровано", user.registeredAt.toDisplayDate())
            InfoRow("Вхід у систему", user.lastLoginAt?.toDisplayDate() ?: "-")
            InfoRow("Кількість входів", user.loginCount.toString())

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Збережені налаштування
            InfoRow("Радіус лопатей", "${user.settings.defaultBladeRadius} м")
            InfoRow("ККД турбіни", "${user.settings.defaultEfficiency} %")
            InfoRow("Анімація турбіни", if (user.settings.turbineAnimationEnabled) "Увімкнено" else "Вимкнено")
            InfoRow("Автозаповнення", if (user.settings.autoFillParameters) "Увімкнено" else "Вимкнено")
        }
    }
}
