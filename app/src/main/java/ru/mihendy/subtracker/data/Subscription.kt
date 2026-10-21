package ru.mihendy.subtracker.data

import android.graphics.Color
import java.time.LocalDate
import java.util.UUID

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
 * @property uid уникальный идентификатор, по умолчанию случайный UUID.
 * @property name название сервиса.
 * @property amount сумма одного списания.
 * @property currency код валюты (ISO 4217), по умолчанию рубли.
 * @property period периодичность списаний.
 * @property nextPaymentDate дата ближайшего списания.
 * @property importance важность подписки.
 * @property color цвет карточки в формате ARGB, по умолчанию белый.
 * @property endDate дата окончания подписки; `null` — бессрочная.
 * @property isActive активна ли подписка (можно приостановить, не удаляя).
 */
data class Subscription(
    val uid: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val currency: String = "RUB",
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextPaymentDate: LocalDate,
    val importance: Importance,
    val color: Int = Color.WHITE,
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

    /** Нужен, чтобы объявить расширение `Subscription.parse` в SubscriptionJson.kt. */
    companion object
}
