package com.tetiana.wind_speed_monitoring.viewmodel.state

//
//  Опис універсальної форми введення.
//  Чистий Kotlin без Compose: ViewModel формує поля, а UniversalFormDialog
//  будує з них UI. Одна форма обслуговує і турбіни, і вимірювання
//

enum class FieldType { Text, Number }

data class FormOption(val id: Long, val label: String)

data class FormField(
    val key: String,
    val label: String,
    val value: String = "",
    val error: String? = null,
    val hint: String? = null,
    val type: FieldType = FieldType.Text,
    val options: List<FormOption> = emptyList(), // непорожній -> поле-вибір (chips)
    val selectedOptionId: Long? = null
) {
    val isChoice: Boolean get() = options.isNotEmpty()
}

data class FormState(
    val title: String,
    val fields: List<FormField>,
    val confirmText: String = "Зберегти",
    val editingId: Long? = null, // null - створення, інакше - редагування
    val isSaving: Boolean = false
) {
    fun value(key: String): String = fields.first { it.key == key }.value
    fun selectedOption(key: String): Long? = fields.first { it.key == key }.selectedOptionId

    // Зміна поля скидає його помилку
    fun withValue(key: String, value: String) =
        copy(fields = fields.map { if (it.key == key) it.copy(value = value, error = null) else it })

    fun withOption(key: String, optionId: Long) =
        copy(fields = fields.map { if (it.key == key) it.copy(selectedOptionId = optionId, error = null) else it })

    fun withErrors(errors: Map<String, String>) =
        copy(fields = fields.map { it.copy(error = errors[it.key]) })
}