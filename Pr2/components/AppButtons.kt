package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//
//  Повторно використовувані кнопки застосунку.
//  Розмір і форма задані в одному місці - щоб змінити дизайн
//  всіх кнопок, достатньо змінити ці константи
//
private val ButtonHeight = 50.dp
private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonFontSize = 16.sp


//    Основна дія екрана (Увійти, Створити акаунт, Зберегти...).
//    isLoading = true - замість тексту показується спінер і loadingText,
//    повторні натискання ігноруються (захист від подвійної відправки).
//    Кнопка лишається кольоровою, щоб індикатор було добре видно
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "Обробка..."
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        shape = ButtonShape
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text(loadingText, fontSize = ButtonFontSize)
        } else {
            Text(text, fontSize = ButtonFontSize)
        }
    }
}


//    Другорядна дія (Вийти з акаунта, Скинути, Назад до меню)
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight),
        shape = ButtonShape
    ) {
        Text(text, fontSize = ButtonFontSize)
    }
}


//    Текстова кнопка-посилання для навігації між екранами.
//    enabled = false під час завантаження, щоб не піти з екрана посеред обробки
@Composable
fun LinkTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}