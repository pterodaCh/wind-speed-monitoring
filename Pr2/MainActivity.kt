package com.tetiana.wind_speed_monitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tetiana.wind_speed_monitoring.auth.AuthScreen
import com.tetiana.wind_speed_monitoring.auth.RegisterScreen
import com.tetiana.wind_speed_monitoring.auth.SessionManager
import com.tetiana.wind_speed_monitoring.composables.MainMenuScreen
import com.tetiana.wind_speed_monitoring.composables.SettingsScreen
import com.tetiana.wind_speed_monitoring.composables.WindApp
import com.tetiana.wind_speed_monitoring.composables.energy.MeasurementsScreen
import com.tetiana.wind_speed_monitoring.composables.energy.TurbinesScreen
import com.tetiana.wind_speed_monitoring.ui.theme.WindspeedmonitoringTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WindspeedmonitoringTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WindAppNavigation()
                }
            }
        }
    }
}

//
//  Маршрути застосунку в одному місці - щоб додати екран,
//  достатньо додати константу і ще один composable() у NavHost
//
object Routes {
    const val AUTH = "auth"
    const val REGISTER = "register"
    const val MENU = "menu"
    const val MONITORING = "monitoring"
    const val SETTINGS = "settings"
    const val TURBINES = "turbines"
    const val MEASUREMENTS = "turbines/{turbineId}"

    fun measurements(turbineId: Long) = "turbines/$turbineId"
}

// Граф навігації: авторизація -> меню -> моніторинг / налаштування / турбіни -> вимірювання
@Composable
fun WindAppNavigation() {
    val navController = rememberNavController()

    // Якщо сесія вже активна (наприклад, після перестворення Activity) - одразу в меню
    val startDestination = if (SessionManager.isLoggedIn) Routes.MENU else Routes.AUTH

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthScreen(
                onLoginSuccess = { navController.openMenuClearingAuth() },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.openMenuClearingAuth() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MENU) {
            MainMenuScreen(
                onOpenMonitoring = { navController.navigate(Routes.MONITORING) },
                onOpenTurbines = { navController.navigate(Routes.TURBINES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLogoutClick = { navController.logout() }
            )
        }

        composable(Routes.MONITORING) {
            WindApp(
                onLogoutClick = { navController.logout() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // База даних турбін (list-detail на планшеті)
        composable(Routes.TURBINES) {
            TurbinesScreen(
                onOpenTurbine = { id -> navController.navigate(Routes.measurements(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Журнал вимірювань окремим екраном (на телефоні)
        composable(
            route = Routes.MEASUREMENTS,
            arguments = listOf(navArgument("turbineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val turbineId = backStackEntry.arguments?.getLong("turbineId") ?: return@composable
            MeasurementsScreen(
                turbineId = turbineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Перехід у меню з видаленням екранів авторизації/реєстрації з історії
private fun NavHostController.openMenuClearingAuth() {
    navigate(Routes.MENU) {
        popUpTo(Routes.AUTH) { inclusive = true }
    }
}

// Вихід: завершуємо сесію і очищаємо весь стек навігації
private fun NavHostController.logout() {
    SessionManager.logout()
    navigate(Routes.AUTH) {
        popUpTo(graph.id) { inclusive = true }
    }
}

@Preview(showBackground = true, name = "Wind App Preview")
@Composable
fun WindAppPreview() {
    WindspeedmonitoringTheme {
        WindApp()
    }
}