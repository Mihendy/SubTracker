package ru.mihendy.subtracker.data

import android.graphics.Color
import org.json.JSONObject
import java.time.DateTimeException
import java.time.LocalDate

// Ключи JSON вынесены в константы, чтобы запись и чтение не разошлись из-за опечатки.
private const val KEY_UID = "uid"
private const val KEY_NAME = "name"
private const val KEY_AMOUNT = "amount"
private const val KEY_CURRENCY = "currency"
private const val KEY_PERIOD = "period"
private const val KEY_NEXT_PAYMENT = "nextPaymentDate"
private const val KEY_IMPORTANCE = "importance"
private const val KEY_COLOR = "color"
private const val KEY_END_DATE = "endDate"
private const val KEY_IS_ACTIVE = "isActive"

private const val DEFAULT_CURRENCY = "RUB"

/**
 * Подписка в виде JSON.
 *
 * Пишутся только примитивы: даты — как `toEpochDay()` (число дней с 1970-01-01),
 * enum — как имя константы, цвет — как Int.
 * Значения по умолчанию (NORMAL, белый цвет, `endDate == null`, RUB, MONTHLY, активна)
 * не пишутся, чтобы файл был компактнее; [parse] подставит их обратно.
 */
val Subscription.json: JSONObject
    get() {
        val json = JSONObject()
        json.put(KEY_UID, uid)
        json.put(KEY_NAME, name)
        json.put(KEY_AMOUNT, amount)
        json.put(KEY_NEXT_PAYMENT, nextPaymentDate.toEpochDay())
        if (currency != DEFAULT_CURRENCY) json.put(KEY_CURRENCY, currency)
        if (period != BillingPeriod.MONTHLY) json.put(KEY_PERIOD, period.name)
        if (importance != Importance.NORMAL) json.put(KEY_IMPORTANCE, importance.name)
        if (color != Color.WHITE) json.put(KEY_COLOR, color)
        endDate?.let { json.put(KEY_END_DATE, it.toEpochDay()) }
        if (!isActive) json.put(KEY_IS_ACTIVE, false)
        return json
    }

/**
 * Восстанавливает подписку из JSON. Никогда не бросает исключений.
 *
 * - нет или битое одно из обязательных полей (`uid`, `name`, `amount`, `nextPaymentDate`) → `null`;
 * - необязательное поле отсутствует или некорректно → значение по умолчанию.
 *
 * `uid` обязателен намеренно: если подставлять новый, одна и та же подписка,
 * пришедшая повторно (например, по сети), получит другой id и задвоится.
 */
fun Subscription.Companion.parse(json: JSONObject): Subscription? {
    val uid = json.opt(KEY_UID) as? String
    if (uid.isNullOrBlank()) return null

    val name = json.opt(KEY_NAME) as? String
    if (name.isNullOrBlank()) return null

    // optDouble вернёт NaN, если поля нет или это не число.
    val amount = json.optDouble(KEY_AMOUNT)
    if (!amount.isFinite()) return null

    val nextPaymentDate = json.optEpochDay(KEY_NEXT_PAYMENT) ?: return null

    val currency = (json.opt(KEY_CURRENCY) as? String)?.takeIf { it.isNotBlank() }
    val period = json.optEnum(KEY_PERIOD) ?: BillingPeriod.MONTHLY
    val importance = json.optEnum(KEY_IMPORTANCE) ?: Importance.NORMAL
    val color = (json.opt(KEY_COLOR) as? Number)?.toInt() ?: Color.WHITE
    val endDate = json.optEpochDay(KEY_END_DATE)
    val isActive = json.opt(KEY_IS_ACTIVE) as? Boolean ?: true

    return Subscription(
        uid = uid,
        name = name,
        amount = amount,
        currency = currency ?: DEFAULT_CURRENCY,
        period = period,
        nextPaymentDate = nextPaymentDate,
        importance = importance,
        color = color,
        endDate = endDate,
        isActive = isActive,
    )
}

/** Читает дату, записанную как epochDay. Нет поля, не число или дата вне диапазона → `null`. */
private fun JSONObject.optEpochDay(key: String): LocalDate? {
    val epochDay = (opt(key) as? Number)?.toLong() ?: return null
    return try {
        LocalDate.ofEpochDay(epochDay)
    } catch (e: DateTimeException) {
        null
    }
}

/** Читает enum по имени константы. Нет поля или неизвестное имя → `null`. */
private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String): T? {
    val name = opt(key) as? String ?: return null
    return enumValues<T>().firstOrNull { it.name == name }
}
