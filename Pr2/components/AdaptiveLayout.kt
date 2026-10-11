package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//
//  Адаптивний layout
//  Тип вікна визначається за доступною шириною (брейкпоінти Material 3):
//  Compact  < 600dp  - телефон у портреті
//  Medium   < 840dp  - телефон в альбомі / малий планшет
//  Expanded ≥ 840dp  - планшет, десктоп
//
enum class WindowType { Compact, Medium, Expanded }

fun windowTypeFor(width: Dp): WindowType = when {
    width < 600.dp -> WindowType.Compact
    width < 840.dp -> WindowType.Medium
    else -> WindowType.Expanded
}

// Зовнішні відступи екрана залежать від його розміру
val WindowType.screenPadding: Dp
    get() = when (this) {
        WindowType.Compact -> 24.dp
        WindowType.Medium -> 32.dp
        WindowType.Expanded -> 48.dp
    }

// Максимальна ширина форми, щоб на планшеті поля не розтягувались на весь екран
val FormMaxWidth = 480.dp


//    Обгортка будь-якого екрана: вимірює доступну ширину
//    і передає у content поточний WindowType.
//    Екрани не знають про пікселі - тільки про тип вікна
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable (WindowType) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        content(windowTypeFor(maxWidth))
    }
}


//    Колонка з прокруткою, яка вміє центрувати вміст по вертикалі.
//    heightIn(min = maxHeight) потрібен, бо всередині verticalScroll
//    висота необмежена і Arrangement.Center інакше не працює
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    padding: Dp = 0.dp,
    centerVertically: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(padding),
            verticalArrangement = if (centerVertically) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}


//    Двопанельний макет.
//    Compact - панелі одна під одною (одна колонка з прокруткою);
//    Medium/Expanded - панелі поруч, кожна з власною прокруткою.
//    Використовується на екранах авторизації, реєстрації, моніторингу та налаштувань
@Composable
fun AdaptiveTwoPane(
    windowType: WindowType,
    modifier: Modifier = Modifier,
    centerVertically: Boolean = true,
    startPane: @Composable ColumnScope.() -> Unit,
    endPane: @Composable ColumnScope.() -> Unit
) {
    if (windowType == WindowType.Compact) {
        ScrollableColumn(
            modifier = modifier.fillMaxSize(),
            padding = windowType.screenPadding,
            centerVertically = centerVertically
        ) {
            startPane()
            Spacer(Modifier.height(32.dp))
            endPane()
        }
    } else {
        Row(modifier = modifier.fillMaxSize().padding(windowType.screenPadding)) {
            ScrollableColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                centerVertically = true,
                content = startPane
            )
            Spacer(Modifier.width(windowType.screenPadding))
            ScrollableColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                centerVertically = centerVertically,
                content = endPane
            )
        }
    }
}
