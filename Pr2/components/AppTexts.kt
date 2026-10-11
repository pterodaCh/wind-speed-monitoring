package com.tetiana.wind_speed_monitoring.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tetiana.wind_speed_monitoring.R

//
//  Повторно використовувані текстові блоки:
//  заголовки екранів, повідомлення/підказки, заголовки секцій, рядки "назва - значення"
//


//    Логотип застосунку (лопаті вітрогенератора).
//    Розмір передається ззовні - на широких екранах логотип більший
@Composable
fun AppLogo(size: Dp = 72.dp) {
    Image(
        painter = painterResource(id = R.drawable.propeller),
        contentDescription = "Логотип моніторингу ВЕС",
        modifier = Modifier.size(size)
    )
}


//    Заголовок екрана з необов'язковим підзаголовком.
//    Використовується на всіх екранах замість дублювання двох Text
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


// Тип повідомлення визначає його колір
enum class MessageType { Error, Info }

//    Повідомлення про помилку або підказка під формою.
//    Якщо повідомлення немає - резервує місце, щоб кнопки не "стрибали"
@Composable
fun MessageText(
    message: String?,
    type: MessageType = MessageType.Error,
    reservedHeight: Dp = 24.dp
) {
    if (message != null) {
        Text(
            text = message,
            color = if (type == MessageType.Error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(reservedHeight))
    }
}


//    Заголовок секції всередині картки чи форми
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}


//    Рядок "назва - значення" для карток зі станом користувача
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}
