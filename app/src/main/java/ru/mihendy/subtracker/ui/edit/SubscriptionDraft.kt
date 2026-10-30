package ru.mihendy.subtracker.ui.edit

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import ru.mihendy.subtracker.data.BillingPeriod
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.data.SubscriptionId
import ru.mihendy.subtracker.data.currencyOrNull
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Черновик подписки — то, что пользователь сейчас видит и правит в форме.
 *
 * Отличается от [Subscription] тем, что может быть невалидным: сумма и код валюты
 * хранятся строками как введены («12,5», «абв», «US»), название может быть пустым.
 * Готовая подписка получается через [toSubscription] только из корректного черновика.
 *
 * @property uid идентификатор подписки; у новой генерируется сразу, чтобы повторное
 *   «Сохранить» обновляло ту же запись, а не создавало копию.
 */
data class SubscriptionDraft(
    val uid: SubscriptionId = SubscriptionId.random(),
    val name: String = "",
    val amountText: String = "",
    val currency: String = Subscription.DEFAULT_CURRENCY.currencyCode,
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextPaymentDate: LocalDate,
    val importance: Importance = Importance.NORMAL,
    val color: Int? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val noteText: String = "",
) {

    /** Сумма из [amountText]; принимает и запятую, и точку. `null` — не число или не больше нуля. */
    val amount: BigDecimal?
        get() = amountText.trim().replace(',', '.').toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 }

    /** Название заполнено (пробелы не считаются). */
    val isNameValid: Boolean
        get() = name.isNotBlank()

    /** Валюта по введённому коду ISO 4217 (RUB, USD, EUR); `null` — такой валюты нет. */
    private val parsedCurrency
        get() = currencyOrNull(currency)

    /** Код валюты существует. */
    val isCurrencyValid: Boolean
        get() = parsedCurrency != null

    /** Подписка из черновика или `null`, если какое-то поле заполнено неверно. */
    fun toSubscription(): Subscription? {
        val amount = amount ?: return null
        val currency = parsedCurrency ?: return null
        if (!isNameValid) return null
        return Subscription(
            uid = uid,
            name = name.trim(),
            amount = amount,
            currency = currency,
            period = period,
            nextPaymentDate = nextPaymentDate,
            importance = importance,
            color = color,
            endDate = endDate,
            isActive = isActive,
            // Пустая заметка — это «заметки нет», а не пустая строка
            note = noteText.trim().ifEmpty { null },
        )
    }

    companion object {

        /** Черновик для редактирования существующей подписки. */
        fun from(subscription: Subscription) = SubscriptionDraft(
            uid = subscription.uid,
            name = subscription.name,
            // toPlainString: 1000 → «1000», а не «1E+3», как может дать toString
            amountText = subscription.amount.toPlainString(),
            currency = subscription.currency.currencyCode,
            period = subscription.period,
            nextPaymentDate = subscription.nextPaymentDate,
            importance = subscription.importance,
            color = subscription.color,
            endDate = subscription.endDate,
            isActive = subscription.isActive,
            noteText = subscription.note.orEmpty(),
        )

        /**
         * Как сохранить черновик в Bundle, чтобы он пережил поворот экрана
         * (используется в `rememberSaveable`).
         *
         * В Bundle можно класть только простые типы, поэтому черновик раскладывается
         * в список строк и чисел (id — тоже строкой), а при восстановлении собирается обратно.
         * `null` (данные ещё не загружены) сохраняется как пустой список.
         */
        val Saver: Saver<SubscriptionDraft?, Any> = listSaver(
            save = { draft ->
                if (draft == null) {
                    emptyList()
                } else {
                    listOf(
                        draft.uid.value,
                        draft.name,
                        draft.amountText,
                        draft.currency,
                        draft.period.name,
                        draft.nextPaymentDate.toEpochDay(),
                        draft.importance.name,
                        draft.color,
                        draft.endDate?.toEpochDay(),
                        draft.isActive,
                        draft.noteText,
                    )
                }
            },
            restore = { list ->
                if (list.isEmpty()) {
                    null
                } else {
                    SubscriptionDraft(
                        uid = SubscriptionId(list[0] as String),
                        name = list[1] as String,
                        amountText = list[2] as String,
                        currency = list[3] as String,
                        period = BillingPeriod.valueOf(list[4] as String),
                        nextPaymentDate = LocalDate.ofEpochDay(list[5] as Long),
                        importance = Importance.valueOf(list[6] as String),
                        color = list[7] as Int?,
                        endDate = (list[8] as Long?)?.let { LocalDate.ofEpochDay(it) },
                        isActive = list[9] as Boolean,
                        noteText = list[10] as String,
                    )
                }
            },
        )
    }
}
