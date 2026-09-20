package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetiana.wind_speed_monitoring.viewmodel.WindViewModel

// Кореневий екран. Збирає дрібні stateless composable-функції
// та прокидає дані/події між ними та WindViewModel.
@Composable
fun WindApp(
    viewModel: WindViewModel = viewModel(),
    onLogoutClick: () -> Unit = {} //  параметр для функції виходу
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val animatedWindSpeed by animateIntAsState(
        targetValue = uiState.windSpeed,
        animationSpec = tween(durationMillis = 1000),
        label = "WindSpeedAnimation"
    )
    val animatedPower by animateIntAsState(
        targetValue = uiState.power,
        animationSpec = tween(durationMillis = 1200),
        label = "PowerAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Параметри ВЕС",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))
        WindmillTurbine(windSpeed = animatedWindSpeed)
        Spacer(Modifier.height(32.dp))

        WindStatusCard(
            windSpeed = animatedWindSpeed,
            power = animatedPower,
            statusLabel = uiState.status.label
        )
        Spacer(Modifier.height(32.dp))

        // Блок з трьома параметрами
        ParameterInputField(
            value = uiState.windSpeedInput,
            onValueChange = viewModel::onWindSpeedChanged,
            label = "Швидкість вітру (м/с)",
            errorText = uiState.windSpeedError
        )
        ParameterInputField(
            value = uiState.bladeRadiusInput,
            onValueChange = viewModel::onBladeRadiusChanged,
            label = "Радіус лопатей (м)",
            errorText = uiState.bladeRadiusError
        )
        ParameterInputField(
            value = uiState.efficiencyInput,
            onValueChange = viewModel::onEfficiencyChanged,
            label = "ККД турбіни (%)",
            errorText = uiState.efficiencyError
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = viewModel::calculateGeneration,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Обчислити генерацію", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Кнопка для повернення на екран авторизації
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Вийти з акаунта", fontSize = 16.sp)
        }
    }
}

// Компонент для вводу параметрів
@Composable
fun ParameterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorText: String?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}