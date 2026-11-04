package ru.mihendy.subtracker.data

import java.math.BigDecimal
import java.util.Currency

/**
 * Денежная сумма: число и валюта вместе.
 *
 * Сумма и валюта по отдельности смысла не имеют: «100» без валюты — непонятно сколько,
 * «RUB» без суммы — вообще не деньги. Поэтому они объединены в один тип, и оба поля
 * обязательные — создать [Money] без валюты нельзя.
 *
 * @property amount число. [BigDecimal], а не Double: деньги нужно хранить точно,
 *   а в Double 0.1 + 0.2 ≠ 0.3.
 * @property currency валюта (ISO 4217).
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) {
    companion object {
        /** Рубли — валюта по умолчанию в форме новой подписки. */
        val RUB: Currency = Currency.getInstance("RUB")
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
