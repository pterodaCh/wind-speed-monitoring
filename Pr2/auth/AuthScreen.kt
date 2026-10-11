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
//  Екран авторизації.
//  Стан полів, помилок і завантаження - remember / rememberSaveable + mutableStateOf.
//  Алгоритм натискання "Увійти":
//   1) AuthValidator перевіряє порожні поля -> помилки під полями;
//   2) якщо помилок немає - isLoading = true, AuthService обробляє дані (корутина);
//   3) успіх -> перехід у меню, помилка -> повідомлення під формою.
//  Compact - одна колонка; Medium/Expanded - дві панелі (логотип | форма)
//
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    // rememberSaveable - як remember, але текст не губиться при повороті екрана
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var fieldErrors by remember { mutableStateOf(LoginErrors()) } // помилки під полями
    var errorMessage by remember { mutableStateOf<String?>(null) } // загальна помилка
    var isLoading by remember { mutableStateOf(false) }           // стан завантаження

    val scope = rememberCoroutineScope()

    val onLoginClick: () -> Unit = {
        val validation = AuthValidator.validateLoginForm(identifier, password)
        fieldErrors = validation
        errorMessage = null
        if (!validation.hasErrors) {
            isLoading = true
            scope.launch {
                when (val result = AuthService.login(identifier, password)) {
                    is AuthResult.Success -> onLoginSuccess()
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
            startPane = { AuthHeader(windowType) },
            endPane = {
                AuthForm(
                    identifier = identifier,
                    onIdentifierChange = {
                        identifier = it
                        fieldErrors = fieldErrors.copy(identifier = null) // Скид помилки поля при введенні
                        errorMessage = null
                    },
                    password = password,
                    onPasswordChange = {
                        password = it
                        fieldErrors = fieldErrors.copy(password = null)
                        errorMessage = null
                    },
                    fieldErrors = fieldErrors,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    onLoginClick = onLoginClick,
                    onRegisterClick = onNavigateToRegister
                )
            }
        )
    }
}

// Компонент: Заголовок (логотип + назва). На широких екранах логотип більший
@Composable
fun AuthHeader(windowType: WindowType = WindowType.Compact) {
    AppLogo(size = if (windowType == WindowType.Compact) 72.dp else 140.dp)
    Spacer(modifier = Modifier.height(16.dp))
    ScreenHeader(
        title = "Авторизація",
        subtitle = "Увійдіть для доступу до моніторингу ВЕС"
    )
}

//    Компонент: Форма (Column з полями, повідомленням, кнопками та підказкою).
//    Обмежена за шириною, щоб на планшеті не розтягуватись на весь екран
@Composable
fun AuthForm(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    fieldErrors: LoginErrors,
    errorMessage: String?,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthInputFields(
            identifier = identifier,
            onIdentifierChange = onIdentifierChange,
            password = password,
            onPasswordChange = onPasswordChange,
            fieldErrors = fieldErrors,
            enabled = !isLoading,
            onDone = onLoginClick // "Готово" на клавіатурі = натискання "Увійти"
        )

        // Загальна помилка (невірні облікові дані)
        MessageText(message = errorMessage)

        AuthButtons(
            isLoading = isLoading,
            onLoginClick = onLoginClick,
            onRegisterClick = onRegisterClick
        )

        AuthHint()
    }
}

//    Компонент: Поля введення.
//    Помилка показується під конкретним полем; поля блокуються під час завантаження
@Composable
fun AuthInputFields(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    fieldErrors: LoginErrors,
    enabled: Boolean,
    onDone: () -> Unit = {}
) {
    AppTextField(
        value = identifier,
        onValueChange = onIdentifierChange,
        label = "Логін або email",
        keyboardType = KeyboardType.Email,
        errorText = fieldErrors.identifier,
        enabled = enabled
    )

    Spacer(modifier = Modifier.height(8.dp))

    AppTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Пароль",
        isPassword = true, // Приховує символи + іконка "показати пароль"
        errorText = fieldErrors.password,
        enabled = enabled,
        imeAction = ImeAction.Done,
        onImeAction = onDone
    )
}

//    Компонент: Кнопки управління.
//    Під час завантаження "Увійти" показує спінер, а перехід до реєстрації вимкнено
@Composable
fun AuthButtons(
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    PrimaryActionButton(
        text = "Увійти",
        onClick = onLoginClick,
        isLoading = isLoading,
        loadingText = "Перевірка даних..."
    )

    Spacer(modifier = Modifier.height(16.dp))

    LinkTextButton(
        text = "Немає акаунту? Зареєструватися",
        onClick = onRegisterClick,
        enabled = !isLoading
    )
}

// Компонент: Підказка з тестовим акаунтом
@Composable
fun AuthHint() {
    MessageText(
        message = "admin - 123456 ",
        type = MessageType.Info
    )
}

@Preview(name = "Авторизація - телефон", showBackground = true, widthDp = 360, heightDp = 740)
@Preview(name = "Авторизація - планшет", showBackground = true, widthDp = 1000, heightDp = 640)
@Composable
fun AuthScreenPreview() {
    WindspeedmonitoringTheme {
        AuthScreen(onLoginSuccess = {}, onNavigateToRegister = {})
    }
}