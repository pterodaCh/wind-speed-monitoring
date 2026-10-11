package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

//
//  Повторно використовувані елементи введення.
//  Один AppTextField замінює всі OutlinedTextField застосунку:
//  логін, email, пароль, числові параметри турбіни
//


//    Універсальне текстове поле.
//    isPassword  - приховує символи та додає іконку "показати пароль";
//    errorText   - червоне повідомлення під полем (має пріоритет над підказкою);
//    hintText    - сіра підказка під полем, що оновлюється під час введення;
//    enabled     - false під час завантаження, щоб дані не змінювались посеред обробки;
//    onImeAction - дія на кнопку "Готово" клавіатури (наприклад, відправити форму)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
    hintText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    // Локальний UI-стан поля: чи показувати пароль.
    // rememberSaveable - переживає поворот екрана
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
        else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Сховати пароль" else "Показати пароль"
                    )
                }
            }
        } else null,
        // Текст під полем виводиться окремим composable
        supportingText = if (errorText != null || hintText != null) {
            { FieldSupportingText(errorText = errorText, hintText = hintText) }
        } else null,
        modifier = modifier.fillMaxWidth()
    )
}


//    Текст під полем: повідомлення про помилку або підказка.
//    Помилка (червона) має пріоритет - підказка показується лише коли помилки немає
@Composable
fun FieldSupportingText(errorText: String?, hintText: String?) {
    when {
        errorText != null -> Text(text = errorText, color = MaterialTheme.colorScheme.error)
        hintText != null -> Text(text = hintText, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


//    Рядок налаштування з перемикачем (назва + опис + Switch).
//    Весь стан передається ззовні - composable stateless
@Composable
fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}