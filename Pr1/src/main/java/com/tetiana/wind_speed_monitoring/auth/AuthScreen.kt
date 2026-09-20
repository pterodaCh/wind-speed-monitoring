package com.tetiana.wind_speed_monitoring.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    // Використання remember та mutableStateOf для динамічного оновлення UI
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthHeader()

        Spacer(modifier = Modifier.height(32.dp))

        // Поля введення
        AuthInputFields(
            login = login,
            onLoginChange = {
                login = it
                errorMessage = null // Скид помилки при введенні
            },
            password = password,
            onPasswordChange = {
                password = it
                errorMessage = null
            }
        )

        // Відображення підказок/помилок
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp)) // Зберігаємо відступ, якщо помилки немає
        }

        // Кнопки дій
        AuthButtons(
            onLoginClick = {
                val validationError = AuthValidator.validateLogin(login, password)
                if (validationError == null) {
                    onLoginSuccess() // Перехід на головний екран
                } else {
                    errorMessage = validationError
                }
            },
            onRegisterClick = onNavigateToRegister
        )
    }
}

// Компонент: Заголовок
@Composable
fun AuthHeader() {
    Text(
        text = "Авторизація",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Увійдіть для доступу до моніторингу ВЕС",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Компонент: Поля введення
@Composable
fun AuthInputFields(
    login: String,
    onLoginChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = login,
        onValueChange = onLoginChange,
        label = { Text("Логін") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Пароль") },
        visualTransformation = PasswordVisualTransformation(), // Приховує символи пароля
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

// Компонент: Кнопки управління
@Composable
fun AuthButtons(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Button(
        onClick = onLoginClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("Увійти", fontSize = 16.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = onRegisterClick) {
        Text("Немає акаунту? Зареєструватися")
    }
}