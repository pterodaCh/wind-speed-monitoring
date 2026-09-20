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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tetiana.wind_speed_monitoring.auth.AuthScreen
import com.tetiana.wind_speed_monitoring.auth.RegisterScreen
import com.tetiana.wind_speed_monitoring.composables.WindApp
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

@Composable
fun WindAppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("monitoring") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("monitoring") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("monitoring") {
            WindApp(
                onLogoutClick = {
                    // При виході переходимо на екран авторизації і видаляємо моніторинг з історії
                    navController.navigate("auth") {
                        popUpTo("monitoring") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Wind App Preview")
@Composable
fun WindAppPreview() {
    WindspeedmonitoringTheme {
        WindApp()
    }
}