package com.tetiana.wind_speed_monitoring.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import kotlinx.coroutines.delay
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.auth.SessionManager
import com.tetiana.wind_speed_monitoring.components.*
import com.tetiana.wind_speed_monitoring.domain.model.SettingsValidator
import com.tetiana.wind_speed_monitoring.domain.model.UserSettings
import com.tetiana.wind_speed_monitoring.ui.theme.WindspeedmonitoringTheme
import kotlinx.coroutines.launch

//
//  Екран налаштувань користувача.
//  Редагує копію налаштувань у локальному стані; кнопка "Зберегти" активна
//  лише якщо є зміни. Після збереження - Snackbar-повідомлення,
//  а картка стану користувача оновлюється автоматично через SessionManager
//
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val savedSettings = SessionManager.currentUser?.settings ?: UserSettings()

    var bladeRadius by rememberSaveable { mutableStateOf(savedSettings.defaultBladeRadius) }
    var efficiency by rememberSaveable { mutableStateOf(savedSettings.defaultEfficiency) }
    var animationEnabled by rememberSaveable { mutableStateOf(savedSettings.turbineAnimationEnabled) }
    var autoFill by rememberSaveable { mutableStateOf(savedSettings.autoFillParameters) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) } // стан завантаження під час збереження

    // Порівнюємо відредаговані налаштування зі збереженими - стан кнопки "Зберегти"
    val editedSettings = UserSettings(bladeRadius, efficiency, animationEnabled, autoFill)
    val hasChanges = editedSettings != savedSettings

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onSaveClick: () -> Unit = {
        val validationError = SettingsValidator.validate(bladeRadius, efficiency)
        if (validationError == null) {
            isSaving = true
            scope.launch {
                delay(800) // імітація збереження на сервері
                SessionManager.updateSettings(editedSettings)
                isSaving = false
                snackbarHostState.showSnackbar("Налаштування збережено")
            }
        } else {
            errorMessage = validationError
        }
    }

    val onResetClick: () -> Unit = {
        val defaults = UserSettings()
        bladeRadius = defaults.defaultBladeRadius
        efficiency = defaults.defaultEfficiency
        animationEnabled = defaults.turbineAnimationEnabled
        autoFill = defaults.autoFillParameters
        errorMessage = null
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        AdaptiveScreen(modifier = Modifier.padding(innerPadding)) { windowType ->
            AdaptiveTwoPane(
                windowType = windowType,
                centerVertically = false,
                startPane = {
                    ScreenHeader(title = "Налаштування", subtitle = "Параметри зберігаються для вашого акаунта")
                    Spacer(Modifier.height(24.dp))
                    UserStatusCard(SessionManager.currentUser)
                },
                endPane = {
                    SettingsForm(
                        bladeRadius = bladeRadius,
                        onBladeRadiusChange = { bladeRadius = it; errorMessage = null },
                        efficiency = efficiency,
                        onEfficiencyChange = { efficiency = it; errorMessage = null },
                        animationEnabled = animationEnabled,
                        onAnimationChange = { animationEnabled = it },
                        autoFill = autoFill,
                        onAutoFillChange = { autoFill = it },
                        errorMessage = errorMessage,
                        isSaveEnabled = hasChanges,
                        isSaving = isSaving,
                        onSaveClick = onSaveClick,
                        onResetClick = onResetClick,
                        onBackClick = onNavigateBack
                    )
                }
            )
        }
    }
}

//    Форма налаштувань: числові параметри, перемикачі та кнопки.
//    Повністю stateless - усі значення та події приходять ззовні.
//    isSaving = true - кнопка "Зберегти" показує спінер,
//    а поля, перемикачі та інші кнопки блокуються до завершення збереження
@Composable
fun SettingsForm(
    bladeRadius: String,
    onBladeRadiusChange: (String) -> Unit,
    efficiency: String,
    onEfficiencyChange: (String) -> Unit,
    animationEnabled: Boolean,
    onAnimationChange: (Boolean) -> Unit,
    autoFill: Boolean,
    onAutoFillChange: (Boolean) -> Unit,
    errorMessage: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onResetClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle("Параметри турбіни за замовчуванням")
        AppTextField(
            value = bladeRadius,
            onValueChange = onBladeRadiusChange,
            label = "Радіус лопатей (м)",
            keyboardType = KeyboardType.Decimal,
            hintText = "Від 0 до 150 м",
            enabled = !isSaving
        )
        Spacer(Modifier.height(8.dp))
        AppTextField(
            value = efficiency,
            onValueChange = onEfficiencyChange,
            label = "ККД турбіни (%)",
            keyboardType = KeyboardType.Decimal,
            hintText = "Не більше межі Беца - 59 %",
            enabled = !isSaving
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("Інтерфейс")
        SwitchSettingRow(
            title = "Анімація турбіни",
            description = "Обертання лопатей на екрані моніторингу",
            checked = animationEnabled,
            onCheckedChange = onAnimationChange,
            enabled = !isSaving
        )
        SwitchSettingRow(
            title = "Автозаповнення",
            description = "Підставляти збережені значення у форму розрахунку",
            checked = autoFill,
            onCheckedChange = onAutoFillChange,
            enabled = !isSaving
        )

        MessageText(message = errorMessage)

        // Під час збереження кнопка лишається активною (щоб було видно спінер),
        // а повторні натискання ігнорує сам PrimaryActionButton
        PrimaryActionButton(
            text = "Зберегти",
            onClick = onSaveClick,
            enabled = isSaveEnabled || isSaving,
            isLoading = isSaving,
            loadingText = "Збереження..."
        )
        Spacer(Modifier.height(12.dp))
        SecondaryActionButton(
            text = "Скинути до стандартних",
            onClick = onResetClick,
            enabled = !isSaving
        )
        Spacer(Modifier.height(8.dp))
        LinkTextButton(
            text = "До головного меню",
            onClick = onBackClick,
            enabled = !isSaving
        )
    }
}

@Preview(name = "Налаштування - телефон", showBackground = true, widthDp = 360, heightDp = 900)
@Preview(name = "Налаштування - планшет", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun SettingsScreenPreview() {
    WindspeedmonitoringTheme {
        SettingsScreen(onNavigateBack = {})
    }
}
