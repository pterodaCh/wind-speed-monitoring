package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.viewmodel.state.OperationState
import com.tetiana.wind_speed_monitoring.viewmodel.state.UiState

//
//  Універсальне відображення UiState: будь-який екран показує
//  завантаження / помилку / дані однаково, просто передаючи свій стан
//
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingView(modifier)
        is UiState.Error -> ErrorView(state.message, onRetry, modifier)
        is UiState.Success -> content(state.data)
    }
}

// Індикатор завантаження по центру
@Composable
fun LoadingView(modifier: Modifier = Modifier, message: String = "Завантаження даних...") {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Помилка з кнопкою повтору
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        PrimaryActionButton(text = "Повторити", onClick = onRetry, modifier = Modifier.widthIn(max = 240.dp))
    }
}

// Порожній стан (немає даних за фільтром / нічого не обрано)
@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Filled.Inbox) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

//    Банер стану разової операції: прогрес / успіх (зелений) / помилка (червоний).
//    Користувач може закрити повідомлення хрестиком
@Composable
fun OperationStatusBanner(state: OperationState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        OperationState.Idle -> Unit
        OperationState.Running -> LinearProgressIndicator(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp))
        is OperationState.Success -> BannerCard(
            state.message, Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
            onDismiss, modifier
        )
        is OperationState.Error -> BannerCard(
            state.message, Icons.Filled.ErrorOutline,
            MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
            onDismiss, modifier
        )
    }
}

@Composable
private fun BannerCard(
    message: String, icon: ImageVector, container: Color, content: Color,
    onDismiss: () -> Unit, modifier: Modifier
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Закрити") }
        }
    }
}