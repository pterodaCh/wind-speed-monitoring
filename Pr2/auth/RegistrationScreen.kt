package com.tetiana.wind_speed_monitoring.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.components.*
import com.tetiana.wind_speed_monitoring.ui.theme.WindspeedmonitoringTheme
import kotlinx.coroutines.launch

//
//  Екран реєстрації з комплексною перевіркою даних.
//  Під час введення: живі підказки, перевірка унікальності логіна/email,
//  шкала надійності пароля та чек-лист вимог.
//  При натисканні: повна перевірка всіх полів (помилки під кожним полем),
//  потім стан завантаження, поки AuthService обробляє дані.
//  Після успішної реєстрації користувач одразу входить у систему
//
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var fieldErrors by remember { mutableStateOf(RegistrationErrors()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Реєстрація: перевірка -> завантаження -> збереження -> старт сесії -> меню
    val onRegisterClick: () -> Unit = {
        val validation = AuthValidator.validateRegistrationForm(login, email, password, confirmPassword)
        fieldErrors = validation
        errorMessage = null
        if (!validation.hasErrors) {
            isLoading = true
            scope.launch {
                when (val result = AuthService.register(login, email, password)) {
                    is AuthResult.Success -> onRegisterSuccess()
                    is AuthResult.Error -> {
                        errorMessage = result.message
                        isLoading = false
                    }
                }
            }
        }
    }

    AdaptiveScreen { windowType ->
        AdaptiveTwoPane(
            windowType = windowType,
            startPane = { RegisterHeader(windowType) },
            endPane = {
                Column(
                    modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RegisterInputFields(
                        login = login,
                        onLoginChange = { login = it; fieldErrors = fieldErrors.copy(login = null); errorMessage = null },
                        email = email,
                        onEmailChange = { email = it; fieldErrors = fieldErrors.copy(email = null); errorMessage = null },
                        password = password,
                        onPasswordChange = { password = it; fieldErrors = fieldErrors.copy(password = null); errorMessage = null },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = {
                            confirmPassword = it
                            fieldErrors = fieldErrors.copy(confirmPassword = null)
                            errorMessage = null
                        },
                        fieldErrors = fieldErrors,
                        enabled = !isLoading,
                        onDone = onRegisterClick
                    )

                    MessageText(message = errorMessage)

                    RegisterButtons(
                        isLoading = isLoading,
                        onRegisterClick = onRegisterClick,
                        onBackClick = onNavigateBack
                    )
                }
            }
        )
    }
}

// Компонент: Заголовок
@Composable
fun RegisterHeader(windowType: WindowType = WindowType.Compact) {
    AppLogo(size = if (windowType == WindowType.Compact) 72.dp else 140.dp)
    Spacer(modifier = Modifier.height(16.dp))
    ScreenHeader(
        title = "Реєстрація",
        subtitle = "Створіть новий обліковий запис"
    )
}

//    Компонент: Поля введення.
//    Під кожним полем: помилка після натискання кнопки (fieldErrors),
//    інакше - жива помилка (зайнятий логін/email), інакше - підказка
@Composable
fun RegisterInputFields(
    login: String, onLoginChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    fieldErrors: RegistrationErrors,
    enabled: Boolean,
    onDone: () -> Unit = {}
) {
    AppTextField(
        value = login,
        onValueChange = onLoginChange,
        label = "Новий логін",
        errorText = fieldErrors.login ?: AuthValidator.loginLiveError(login),
        hintText = AuthValidator.loginHint(login),
        enabled = enabled
    )
    Spacer(modifier = Modifier.height(8.dp))
    AppTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        keyboardType = KeyboardType.Email,
        errorText = fieldErrors.email ?: AuthValidator.emailLiveError(email),
        hintText = AuthValidator.emailHint(email),
        enabled = enabled
    )
    Spacer(modifier = Modifier.height(8.dp))
    RegisterPasswordSection(
        password = password,
        onPasswordChange = onPasswordChange,
        errorText = fieldErrors.password,
        enabled = enabled
    )
    AppTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "Повторіть пароль",
        isPassword = true,
        errorText = fieldErrors.confirmPassword ?: AuthValidator.confirmPasswordLiveError(password, confirmPassword),
        enabled = enabled,
        imeAction = ImeAction.Done,
        onImeAction = onDone
    )
}

//    Компонент: Поле пароля + індикатор надійності та чек-лист вимог.
//    Рівень і вимоги обчислює PasswordPolicy з поточного значення пароля
@Composable
fun RegisterPasswordSection(
    password: String,
    onPasswordChange: (String) -> Unit,
    errorText: String?,
    enabled: Boolean
) {
    AppTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Пароль",
        isPassword = true,
        errorText = errorText,
        enabled = enabled
    )
    PasswordStrengthIndicator(
        strength = PasswordPolicy.strengthOf(password),
        requirements = PasswordPolicy.requirementStates(password)
    )
}

//    Компонент: Кнопки.
//    Кнопка завжди доступна, щоб при порожніх полях показати помилки під ними;
//    під час обробки показує спінер і блокує повернення назад
@Composable
fun RegisterButtons(
    isLoading: Boolean,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    PrimaryActionButton(
        text = "Створити акаунт",
        onClick = onRegisterClick,
        isLoading = isLoading,
        loadingText = "Створення акаунта..."
    )
    Spacer(modifier = Modifier.height(16.dp))
    LinkTextButton(text = "Вже є акаунт? Увійти", onClick = onBackClick, enabled = !isLoading)
}

@Preview(name = "Реєстрація - телефон", showBackground = true, widthDp = 360, heightDp = 1000)
@Preview(name = "Реєстрація - планшет", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun RegisterScreenPreview() {
    WindspeedmonitoringTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateBack = {})
    }
}