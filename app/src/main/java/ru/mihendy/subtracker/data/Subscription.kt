package ru.mihendy.subtracker.data

import java.time.LocalDate

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
 * @property price сколько списывается за один раз — сумма вместе с валютой, см. [Money].
 * @property period периодичность списаний.
 * @property nextPaymentDate дата ближайшего списания.
 * @property importance важность подписки.
 * @property color цветовая метка в формате ARGB; `null` — метки нет.
 *   Именно `null`, а не белый по умолчанию: иначе «метки нет» и «выбран белый»
 *   стали бы неразличимы.
 * @property endDate дата окончания подписки; `null` — бессрочная.
 * @property isActive активна ли подписка (можно приостановить, не удаляя).
 * @property note заметка в свободной форме; `null` — заметки нет.
 */
data class Subscription(
    val uid: SubscriptionId = SubscriptionId.random(),
    val name: String,
    val price: Money,
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextPaymentDate: LocalDate,
    val importance: Importance,
    val color: Int? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val note: String? = null,
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

    /** Нужен, чтобы объявить расширение `Subscription.parse` в SubscriptionJson.kt. */
    companion object
}
