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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegisterHeader()

        Spacer(modifier = Modifier.height(32.dp))

        RegisterInputFields(
            login = login,
            onLoginChange = { login = it; errorMessage = null },
            password = password,
            onPasswordChange = { password = it; errorMessage = null },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it; errorMessage = null }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        RegisterButtons(
            onRegisterClick = {
                val validationError = AuthValidator.validateRegistration(login, password, confirmPassword)
                if (validationError == null) {
                    // ТЕПЕР КОРИСТУВАЧ ЗБЕРІГАЄТЬСЯ
                    UserRepository.register(login, password)
                    onRegisterSuccess()
                } else {
                    errorMessage = validationError
                }
            },
            onBackClick = onNavigateBack
        )
    }
}

@Composable
fun RegisterHeader() {
    Text(
        text = "Реєстрація",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Створіть новий обліковий запис",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun RegisterInputFields(
    login: String, onLoginChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = login,
        onValueChange = onLoginChange,
        label = { Text("Новий логін") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Пароль") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = { Text("Повторіть пароль") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun RegisterButtons(
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Button(
        onClick = onRegisterClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("Створити акаунт", fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onBackClick) {
        Text("Вже є акаунт? Увійти")
    }
}