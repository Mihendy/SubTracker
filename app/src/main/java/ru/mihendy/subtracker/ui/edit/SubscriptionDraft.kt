package ru.mihendy.subtracker.ui.edit

import android.graphics.Color
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import ru.mihendy.subtracker.data.BillingPeriod
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Subscription
import java.time.LocalDate
import java.util.UUID

/**
 * Черновик подписки — то, что пользователь сейчас видит и правит в форме.
 *
 * Отличается от [Subscription] тем, что может быть невалидным: сумма хранится
 * строкой как введена («12,5», «» или «абв»), название может быть пустым.
 * Готовая подписка получается через [toSubscription] только из корректного черновика.
 *
 * @property uid идентификатор подписки; у новой генерируется сразу, чтобы повторное
 *   «Сохранить» обновляло ту же запись, а не создавало копию.
 */
data class SubscriptionDraft(
    val uid: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amountText: String = "",
    val currency: String = DEFAULT_CURRENCY,
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextPaymentDate: LocalDate,
    val importance: Importance = Importance.NORMAL,
    val color: Int = Color.WHITE,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
) {

    /** Сумма из [amountText]; принимает и запятую, и точку. `null` — не число или не больше нуля. */
    val amount: Double?
        get() = amountText.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0 }

    /** Название заполнено (пробелы не считаются). */
    val isNameValid: Boolean
        get() = name.isNotBlank()

    /** Код валюты — ровно три латинские буквы (формат ISO 4217: RUB, USD, EUR). */
    val isCurrencyValid: Boolean
        get() = CURRENCY_REGEX.matches(currency)

    /** Подписка из черновика или `null`, если какое-то поле заполнено неверно. */
    fun toSubscription(): Subscription? {
        val amount = amount ?: return null
        if (!isNameValid || !isCurrencyValid) return null
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
        )
    }

    companion object {
        const val DEFAULT_CURRENCY = "RUB"
        private val CURRENCY_REGEX = Regex("[A-Z]{3}")

        /** Черновик для редактирования существующей подписки. */
        fun from(subscription: Subscription) = SubscriptionDraft(
            uid = subscription.uid,
            name = subscription.name,
            amountText = formatAmount(subscription.amount),
            currency = subscription.currency,
            period = subscription.period,
            nextPaymentDate = subscription.nextPaymentDate,
            importance = subscription.importance,
            color = subscription.color,
            endDate = subscription.endDate,
            isActive = subscription.isActive,
        )

        /** 199.0 → «199», 2.99 → «2.99»: без лишнего «.0» у целых сумм. */
        private fun formatAmount(amount: Double): String =
            if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

        /**
         * Как сохранить черновик в Bundle, чтобы он пережил поворот экрана
         * (используется в `rememberSaveable`).
         *
         * В Bundle можно класть только простые типы, поэтому черновик раскладывается
         * в список строк и чисел, а при восстановлении собирается обратно.
         * `null` (данные ещё не загружены) сохраняется как пустой список.
         */
        val Saver: Saver<SubscriptionDraft?, Any> = listSaver(
            save = { draft ->
                if (draft == null) {
                    emptyList()
                } else {
                    listOf(
                        draft.uid,
                        draft.name,
                        draft.amountText,
                        draft.currency,
                        draft.period.name,
                        draft.nextPaymentDate.toEpochDay(),
                        draft.importance.name,
                        draft.color,
                        draft.endDate?.toEpochDay(),
                        draft.isActive,
                    )
                }
            },
            restore = { list ->
                if (list.isEmpty()) {
                    null
                } else {
                    SubscriptionDraft(
                        uid = list[0] as String,
                        name = list[1] as String,
                        amountText = list[2] as String,
                        currency = list[3] as String,
                        period = BillingPeriod.valueOf(list[4] as String),
                        nextPaymentDate = LocalDate.ofEpochDay(list[5] as Long),
                        importance = Importance.valueOf(list[6] as String),
                        color = list[7] as Int,
                        endDate = (list[8] as Long?)?.let { LocalDate.ofEpochDay(it) },
                        isActive = list[9] as Boolean,
                    )
                }
            },
        )
    }
}
