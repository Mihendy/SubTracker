package ru.mihendy.subtracker.data

import org.json.JSONObject
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate

// Ключи JSON вынесены в константы, чтобы запись и чтение не разошлись из-за опечатки.
private const val KEY_UID = "uid"
private const val KEY_NAME = "name"
private const val KEY_PRICE = "price"
private const val KEY_AMOUNT = "amount"
private const val KEY_CURRENCY = "currency"
private const val KEY_PERIOD = "period"
private const val KEY_NEXT_PAYMENT = "nextPaymentDate"
private const val KEY_IMPORTANCE = "importance"
private const val KEY_COLOR = "color"
private const val KEY_END_DATE = "endDate"
private const val KEY_IS_ACTIVE = "isActive"
private const val KEY_NOTE = "note"

/**
 * Подписка в виде JSON.
 *
 * Пишутся только примитивы: id — строкой, даты — как `toEpochDay()` (число дней
 * с 1970-01-01), enum — как имя константы, цвет — как Int.
 * Цена — вложенным объектом `{"amount": "2.99", "currency": "USD"}`: сумма и валюта
 * неразделимы (см. [Money]), поэтому и в файле лежат вместе и всегда обе. Сумма — строкой,
 * чтобы не терять точность на Double.
 * Значения по умолчанию (NORMAL, без цвета, `endDate == null`, MONTHLY, активна, без заметки)
 * не пишутся, чтобы файл был компактнее; [parse] подставит их обратно.
 */
val Subscription.json: JSONObject
    get() {
        val json = JSONObject()
        json.put(KEY_UID, uid.value)
        json.put(KEY_NAME, name)
        json.put(KEY_PRICE, price.json)
        json.put(KEY_NEXT_PAYMENT, nextPaymentDate.toEpochDay())
        if (period != BillingPeriod.MONTHLY) json.put(KEY_PERIOD, period.name)
        if (importance != Importance.NORMAL) json.put(KEY_IMPORTANCE, importance.name)
        color?.let { json.put(KEY_COLOR, it) }
        endDate?.let { json.put(KEY_END_DATE, it.toEpochDay()) }
        if (!isActive) json.put(KEY_IS_ACTIVE, false)
        note?.let { json.put(KEY_NOTE, it) }
        return json
    }

/**
 * Восстанавливает подписку из JSON. Никогда не бросает исключений.
 *
 * - нет или битое одно из обязательных полей (`uid`, `name`, `price`, `nextPaymentDate`) → `null`;
 * - в цене нет валюты или код неизвестен → `null`: молча подставить рубли значило бы
 *   превратить 10 $ в 10 ₽;
 * - старый формат (сумма и валюта отдельными полями верхнего уровня) тоже читается,
 *   см. [optLegacyPrice];
 * - другое необязательное поле отсутствует или некорректно → значение по умолчанию.
 *
 * `uid` обязателен намеренно: если подставлять новый, одна и та же подписка,
 * пришедшая повторно (например, по сети), получит другой id и задвоится.
 */
fun Subscription.Companion.parse(json: JSONObject): Subscription? {
    val uid = json.opt(KEY_UID) as? String
    if (uid.isNullOrBlank()) return null

    val name = json.opt(KEY_NAME) as? String
    if (name.isNullOrBlank()) return null

    val price = json.optPrice() ?: return null
    val nextPaymentDate = json.optEpochDay(KEY_NEXT_PAYMENT) ?: return null

    val period = json.optEnum(KEY_PERIOD) ?: BillingPeriod.MONTHLY
    val importance = json.optEnum(KEY_IMPORTANCE) ?: Importance.NORMAL
    val color = (json.opt(KEY_COLOR) as? Number)?.toInt()
    val endDate = json.optEpochDay(KEY_END_DATE)
    val isActive = json.opt(KEY_IS_ACTIVE) as? Boolean ?: true
    val note = (json.opt(KEY_NOTE) as? String)?.takeIf { it.isNotBlank() }

    return Subscription(
        uid = SubscriptionId(uid),
        name = name,
        price = price,
        period = period,
        nextPaymentDate = nextPaymentDate,
        importance = importance,
        color = color,
        endDate = endDate,
        isActive = isActive,
        note = note,
    )
}

/** Цена в виде JSON: `{"amount": "2.99", "currency": "USD"}`. */
private val Money.json: JSONObject
    get() = JSONObject()
        .put(KEY_AMOUNT, amount.toPlainString())
        .put(KEY_CURRENCY, currency.currencyCode)

/**
 * Читает цену. Поле `price` есть → разбираем его, и тут сумма и валюта обязательны обе.
 * Поля нет → это файл старого формата, читаем через [optLegacyPrice].
 */
private fun JSONObject.optPrice(): Money? {
    if (!has(KEY_PRICE)) return optLegacyPrice()
    val price = opt(KEY_PRICE) as? JSONObject ?: return null
    val amount = price.optAmount() ?: return null
    val currency = (price.opt(KEY_CURRENCY) as? String)?.let(::currencyOrNull) ?: return null
    return Money(amount, currency)
}

/**
 * Старый формат: `amount` и `currency` лежали прямо в подписке, а рубли (значение
 * по умолчанию) не писались вовсе. Поэтому здесь, и только здесь, отсутствие валюты
 * означает рубли: так эти файлы и записывались. Неизвестный код валюты → `null`.
 */
private fun JSONObject.optLegacyPrice(): Money? {
    val amount = optAmount() ?: return null
    val currency = when (val raw = opt(KEY_CURRENCY)) {
        null -> Money.RUB
        is String -> currencyOrNull(raw)
        else -> null
    } ?: return null
    return Money(amount, currency)
}

/**
 * Читает сумму. Сейчас она пишется строкой («2.99»), но самые старые файлы хранят её
 * числом (199.0) — читаем оба варианта. Нет поля или не число → `null`.
 */
private fun JSONObject.optAmount(): BigDecimal? = when (val raw = opt(KEY_AMOUNT)) {
    is String -> raw.toBigDecimalOrNull()
    is Number -> raw.toString().toBigDecimalOrNull()
    else -> null
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
