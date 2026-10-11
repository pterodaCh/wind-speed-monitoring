package com.tetiana.wind_speed_monitoring.viewmodel.state

//
//  Стан даних екрана (MVVM): ViewModel публікує один з трьох станів,
//  а UI лише відображає його - без власної логіки завантаження
//
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> T.asSuccess(): UiState<T> = UiState.Success(this)

//
//  Стан разової операції (запит до сенсора): окремо від даних екрана,
//  щоб збій сенсора не "ламав" весь список
//
sealed interface OperationState {
    data object Idle : OperationState
    data object Running : OperationState
    data class Success(val message: String) : OperationState
    data class Error(val message: String) : OperationState
}