package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetiana.wind_speed_monitoring.auth.SessionManager
import com.tetiana.wind_speed_monitoring.components.AdaptiveScreen
import com.tetiana.wind_speed_monitoring.components.AdaptiveTwoPane
import com.tetiana.wind_speed_monitoring.components.AppTextField
import com.tetiana.wind_speed_monitoring.components.FormMaxWidth
import com.tetiana.wind_speed_monitoring.components.PrimaryActionButton
import com.tetiana.wind_speed_monitoring.components.SecondaryActionButton
import com.tetiana.wind_speed_monitoring.components.SectionTitle
import com.tetiana.wind_speed_monitoring.domain.model.UserSettings
import com.tetiana.wind_speed_monitoring.domain.model.WindTurbineUiState
import com.tetiana.wind_speed_monitoring.viewmodel.WindViewModel

// Кореневий екран моніторингу. Збирає дрібні stateless composable-функції
// та прокидає дані/події між ними та WindViewModel.
// Compact - усе в одну колонку; ширші екрани - турбіна і показники ліворуч,
// форма параметрів праворуч
@Composable
fun WindApp(
    viewModel: WindViewModel = viewModel(),
    onLogoutClick: () -> Unit = {}, // параметр для функції виходу
    onNavigateBack: () -> Unit = {} // повернення в головне меню
) {
    val uiState by viewModel.uiState.collectAsState()

    // Збережені налаштування поточного користувача (State з SessionManager)
    val settings = SessionManager.currentUser?.settings ?: UserSettings()

    // Інтеграція з налаштуваннями: автозаповнюємо порожні поля
    // збереженими значеннями (виконується при вході на екран і при зміні налаштувань)
    LaunchedEffect(settings) {
        if (settings.autoFillParameters) {
            val current = viewModel.uiState.value
            if (current.bladeRadiusInput.isBlank()) viewModel.onBladeRadiusChanged(settings.defaultBladeRadius)
            if (current.efficiencyInput.isBlank()) viewModel.onEfficiencyChanged(settings.defaultEfficiency)
        }
    }

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

    AdaptiveScreen { windowType ->
        AdaptiveTwoPane(
            windowType = windowType,
            centerVertically = false,
            startPane = {
                MonitoringHeader()
                Spacer(Modifier.height(16.dp))
                // Якщо анімацію вимкнено в налаштуваннях - передаємо 0,
                // і WindmillTurbine показує нерухомі лопаті
                WindmillTurbine(windSpeed = if (settings.turbineAnimationEnabled) animatedWindSpeed else 0)
                Spacer(Modifier.height(32.dp))
                WindStatusCard(
                    windSpeed = animatedWindSpeed,
                    power = animatedPower,
                    statusLabel = uiState.status.label
                )
            },
            endPane = {
                ParametersForm(
                    uiState = uiState,
                    onWindSpeedChange = viewModel::onWindSpeedChanged,
                    onBladeRadiusChange = viewModel::onBladeRadiusChanged,
                    onEfficiencyChange = viewModel::onEfficiencyChanged,
                    onCalculateClick = viewModel::calculateGeneration
                )
                Spacer(Modifier.height(16.dp))
                MonitoringNavigationButtons(
                    onBackClick = onNavigateBack,
                    onLogoutClick = onLogoutClick
                )
            }
        )
    }
}

// Компонент: Заголовок екрана моніторингу
@Composable
fun MonitoringHeader() {
    Text(
        text = "Параметри ВЕС",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

//    Компонент: Блок з трьома параметрами + кнопка розрахунку.
//    Stateless - значення і помилки беруться з WindTurbineUiState,
//    події віддаються назовні (у ViewModel)
@Composable
fun ParametersForm(
    uiState: WindTurbineUiState,
    onWindSpeedChange: (String) -> Unit,
    onBladeRadiusChange: (String) -> Unit,
    onEfficiencyChange: (String) -> Unit,
    onCalculateClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle("Вхідні дані")
        ParameterInputField(
            value = uiState.windSpeedInput,
            onValueChange = onWindSpeedChange,
            label = "Швидкість вітру (м/с)",
            errorText = uiState.windSpeedError
        )
        ParameterInputField(
            value = uiState.bladeRadiusInput,
            onValueChange = onBladeRadiusChange,
            label = "Радіус лопатей (м)",
            errorText = uiState.bladeRadiusError
        )
        ParameterInputField(
            value = uiState.efficiencyInput,
            onValueChange = onEfficiencyChange,
            label = "ККД турбіни (%)",
            errorText = uiState.efficiencyError
        )

        Spacer(Modifier.height(16.dp))

        PrimaryActionButton(text = "Обчислити генерацію", onClick = onCalculateClick)
    }
}

// Компонент для вводу параметрів - числове поле на основі спільного AppTextField
@Composable
fun ParameterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorText: String?
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardType = KeyboardType.Number,
        errorText = errorText
    )
    Spacer(Modifier.height(8.dp))
}

// Компонент: Навігаційні кнопки (у меню / вихід з акаунта)
@Composable
fun MonitoringNavigationButtons(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth()) {
        SecondaryActionButton(text = "До головного меню", onClick = onBackClick)
        Spacer(Modifier.height(12.dp))
        // Кнопка для повернення на екран авторизації
        SecondaryActionButton(text = "Вийти з акаунта", onClick = onLogoutClick)
    }
}
