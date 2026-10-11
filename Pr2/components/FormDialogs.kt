package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.viewmodel.state.FieldType
import com.tetiana.wind_speed_monitoring.viewmodel.state.FormField
import com.tetiana.wind_speed_monitoring.viewmodel.state.FormState

//
//  Універсальна форма введення в діалозі.
//  Будує поля з FormState, який підготувала ViewModel: текстові, числові
//  та поля вибору. Одна реалізація - для турбін і для вимірювань.
//  Під час збереження поля блокуються, а кнопка показує спінер
//
@Composable
fun UniversalFormDialog(
    state: FormState,
    onValueChange: (key: String, value: String) -> Unit,
    onOptionSelect: (key: String, optionId: Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                state.fields.forEach { field ->
                    FormFieldInput(
                        field = field,
                        enabled = !state.isSaving,
                        onValueChange = { onValueChange(field.key, it) },
                        onOptionSelect = { onOptionSelect(field.key, it) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Збереження...")
                } else {
                    Text(state.confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Скасувати") }
        }
    )
}

//    Одне поле форми: вибір (chips) або текст/число (AppTextField)
@Composable
fun FormFieldInput(
    field: FormField,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onOptionSelect: (Long) -> Unit
) {
    if (field.isChoice) {
        Column {
            Text(field.label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FilterChipsRow(
                options = field.options,
                selected = field.options.firstOrNull { it.id == field.selectedOptionId },
                label = { it.label },
                onSelect = { onOptionSelect(it.id) },
                enabled = enabled
            )
            field.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        AppTextField(
            value = field.value,
            onValueChange = onValueChange,
            label = field.label,
            keyboardType = if (field.type == FieldType.Number) KeyboardType.Decimal else KeyboardType.Text,
            errorText = field.error,
            hintText = field.hint,
            enabled = enabled
        )
    }
}

//    Підтвердження видалення - захист від випадкової втрати даних
@Composable
fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Видалити", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}