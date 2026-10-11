package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.auth.SessionManager
import com.tetiana.wind_speed_monitoring.components.*
import com.tetiana.wind_speed_monitoring.domain.model.User
import com.tetiana.wind_speed_monitoring.ui.theme.WindspeedmonitoringTheme

//
//  Опис пункту меню.
//  Щоб додати новий розділ, достатньо додати ще один MenuEntry у список нижче
//
data class MenuEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

//
//  Головне меню після входу.
//  Показує привітання, стан користувача (з SessionManager) та розділи застосунку.
//  Кількість колонок меню залежить від ширини екрана
//
@Composable
fun MainMenuScreen(
    onOpenMonitoring: () -> Unit,
    onOpenTurbines: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // Читання State з SessionManager - меню перемалюється при зміні користувача
    val user = SessionManager.currentUser

    val menuEntries = listOf(
        MenuEntry("Моніторинг ВЕС", "Розрахунок генерації та стан турбіни", Icons.Filled.Air, onOpenMonitoring),
        MenuEntry("Турбіни та вимірювання", "База даних: CRUD, фільтри, звіти станцій", Icons.Filled.Storage, onOpenTurbines),
        MenuEntry("Налаштування", "Параметри турбіни за замовчуванням", Icons.Filled.Settings, onOpenSettings)
    )

    AdaptiveScreen { windowType ->
        MainMenuContent(
            user = user,
            menuEntries = menuEntries,
            windowType = windowType,
            onLogoutClick = onLogoutClick
        )
    }
}

// Вміст меню окремо від джерела даних - так його легко показати в Preview
@Composable
fun MainMenuContent(
    user: User?,
    menuEntries: List<MenuEntry>,
    windowType: WindowType,
    onLogoutClick: () -> Unit
) {
    ScrollableColumn(
        modifier = Modifier.fillMaxSize(),
        padding = windowType.screenPadding,
        centerVertically = false
    ) {
        Column(
            modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainMenuHeader(user)
            Spacer(Modifier.height(24.dp))

            // Compact - картка над меню; ширші екрани - картка ліворуч, меню праворуч
            if (windowType == WindowType.Compact) {
                UserStatusCard(user)
                Spacer(Modifier.height(24.dp))
                MenuGrid(entries = menuEntries, columns = 1)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    UserStatusCard(user, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f)) {
                        MenuGrid(entries = menuEntries, columns = 1)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            SecondaryActionButton(
                text = "Вийти з акаунта",
                onClick = onLogoutClick,
                modifier = Modifier.widthIn(max = FormMaxWidth)
            )
        }
    }
}

// Компонент: Привітання користувача
@Composable
fun MainMenuHeader(user: User?) {
    AppLogo(size = 64.dp)
    Spacer(Modifier.height(12.dp))
    ScreenHeader(
        title = "Головне меню",
        subtitle = if (user != null) "Вітаємо, ${user.login}!" else null
    )
}

//    Сітка пунктів меню. Розбиває список на рядки по [columns] карток.
//    Порожні місця в останньому рядку заповнюються Spacer, щоб картки
//    мали однакову ширину
@Composable
fun MenuGrid(entries: List<MenuEntry>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowEntries.forEach { entry ->
                    MenuItemCard(
                        title = entry.title,
                        description = entry.description,
                        icon = entry.icon,
                        onClick = entry.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowEntries.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val previewUser = User(login = "tetiana", password = "", loginCount = 3, lastLoginAt = System.currentTimeMillis())
private val previewEntries = listOf(
    MenuEntry("Моніторинг ВЕС", "Розрахунок генерації та стан турбіни", Icons.Filled.Air) {},
    MenuEntry("Налаштування", "Параметри турбіни за замовчуванням", Icons.Filled.Settings) {}
)

@Preview(name = "Меню - телефон", showBackground = true, widthDp = 360, heightDp = 900)
@Composable
fun MainMenuPhonePreview() {
    WindspeedmonitoringTheme {
        MainMenuContent(previewUser, previewEntries, WindowType.Compact, onLogoutClick = {})
    }
}

@Preview(name = "Меню - планшет", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun MainMenuTabletPreview() {
    WindspeedmonitoringTheme {
        MainMenuContent(previewUser, previewEntries, WindowType.Expanded, onLogoutClick = {})
    }
}
