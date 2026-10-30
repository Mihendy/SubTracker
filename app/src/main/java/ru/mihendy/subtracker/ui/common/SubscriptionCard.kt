package ru.mihendy.subtracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Насколько прозрачной становится карточка приостановленной подписки. */
private const val INACTIVE_ALPHA = 0.45f

/**
 * Карточка подписки: полоска цветовой метки слева, название, сумма, дата списания.
 *
 * Приостановленная подписка ([isActive] == `false`) «сереет»: полоска становится
 * серой, вся карточка — полупрозрачной, появляется подпись «Приостановлена».
 *
 * Принимает уже готовые для показа значения, а не модель, — поэтому одна и та же
 * карточка подходит и для превью в форме (черновик), и для будущего списка.
 *
 * @param amount сумма как текст; `null` — суммы пока нет (в форме ещё не введена).
 * @param color цвет метки (ARGB); `null` — метки нет, полоска не рисуется.
 */
@Composable
fun SubscriptionCard(
    name: String,
    amount: String?,
    currency: String,
    nextPaymentDate: LocalDate,
    color: Int?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val stripeColor = when {
        !isActive -> MaterialTheme.colorScheme.outline
        color != null -> Color(color)
        else -> Color.Transparent
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else INACTIVE_ALPHA),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // IntrinsicSize.Min — чтобы полоска растянулась ровно на высоту текста
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(stripeColor),
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = name.ifBlank { stringResource(R.string.card_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${amount ?: stringResource(R.string.card_no_amount)} $currency",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.card_next_payment, nextPaymentDate.format(formatter)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isActive) {
                    Text(
                        text = stringResource(R.string.card_paused),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Активная")
@Composable
private fun ActiveCardPreview() {
    SubTrackerTheme(dynamicColor = false) {
        SubscriptionCard(
            name = "Музыка",
            amount = "199",
            currency = "RUB",
            nextPaymentDate = LocalDate.of(2026, 11, 15),
            color = 0xFF81C784.toInt(),
            isActive = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Приостановлена")
@Composable
private fun InactiveCardPreview() {
    SubTrackerTheme(dynamicColor = false) {
        SubscriptionCard(
            name = "Музыка",
            amount = "199",
            currency = "RUB",
            nextPaymentDate = LocalDate.of(2026, 11, 15),
            color = 0xFF81C784.toInt(),
            isActive = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
