package ru.mihendy.subtracker.data

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

/** Насколько подписка важна для пользователя. */
enum class Importance {
    /** Можно отказаться без потерь. */
    OPTIONAL,

    /** Обычная подписка (значение по умолчанию). */
    NORMAL,

    /** Без неё никак. */
    ESSENTIAL,
}

/** Как часто происходит списание. */
enum class BillingPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/**
 * Подписка или регулярный платёж.
 *
 * Неизменяемая модель: все поля `val`, любое изменение через [copy].
 *
 * @property uid уникальный идентификатор, по умолчанию случайный.
 * @property name название сервиса.
 * @property amount сумма одного списания. [BigDecimal], а не Double: деньги нужно хранить
 *   точно, а в Double 0.1 + 0.2 ≠ 0.3.
 * @property currency валюта (ISO 4217), по умолчанию рубли.
 * @property period периодичность списаний.
 * @property nextPaymentDate дата ближайшего списания.
 * @property importance важность подписки.
 * @property color цветовая метка в формате ARGB; `null` — метки нет.
 *   Именно `null`, а не белый по умолчанию: иначе «метки нет» и «выбран белый»
 *   стали бы неразличимы.
 * @property endDate дата окончания подписки; `null` — бессрочная.
 * @property isActive активна ли подписка (можно приостановить, не удаляя).
 */
data class Subscription(
    val uid: SubscriptionId = SubscriptionId.random(),
    val name: String,
    val amount: BigDecimal,
    val currency: Currency = DEFAULT_CURRENCY,
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextPaymentDate: LocalDate,
    val importance: Importance,
    val color: Int? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
) {

    /**
     * Возвращает копию, у которой дата следующего списания сдвинута на один период.
     *
     * Для месячного периода используется [LocalDate.plusMonths]: 31 января → 28 (29) февраля.
     */
    fun nextCycle(): Subscription {
        val next = when (period) {
            BillingPeriod.WEEKLY -> nextPaymentDate.plusWeeks(1)
            BillingPeriod.MONTHLY -> nextPaymentDate.plusMonths(1)
            BillingPeriod.YEARLY -> nextPaymentDate.plusYears(1)
        }
        return copy(nextPaymentDate = next)
    }

    /**
     * Пролистывает уже прошедшие списания: сдвигает дату на целое число периодов,
     * пока она не станет не раньше [today]. Списание в сам день [today] прошедшим не считается.
     * Если дата и так не в прошлом — возвращается этот же объект.
     */
    fun rolledForward(today: LocalDate): Subscription {
        var current = this
        while (current.nextPaymentDate.isBefore(today)) {
            current = current.nextCycle()
        }
        return current
    }

    /** Ещё нужен, чтобы объявить расширение `Subscription.parse` в SubscriptionJson.kt. */
    companion object {
        /** Валюта по умолчанию — рубли. */
        val DEFAULT_CURRENCY: Currency = Currency.getInstance("RUB")
    }
}

/**
 * Валюта по коду ISO 4217 («RUB», «USD»). Неизвестный код или неверный регистр → `null`.
 *
 * Обёртка нужна, потому что [Currency.getInstance] на неизвестный код бросает исключение.
 */
fun currencyOrNull(code: String): Currency? = try {
    Currency.getInstance(code)
} catch (e: IllegalArgumentException) {
    null
}
