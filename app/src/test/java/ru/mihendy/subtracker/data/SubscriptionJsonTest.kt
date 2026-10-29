package ru.mihendy.subtracker.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

class SubscriptionJsonTest {

    private val minimal = Subscription(
        name = "Музыка",
        amount = BigDecimal("199"),
        nextPaymentDate = LocalDate.of(2026, 11, 1),
        importance = Importance.NORMAL,
    )

    private val full = Subscription(
        uid = SubscriptionId("fixed-uid"),
        name = "Облако",
        amount = BigDecimal("9.99"),
        currency = Currency.getInstance("USD"),
        period = BillingPeriod.YEARLY,
        nextPaymentDate = LocalDate.of(2027, 1, 15),
        importance = Importance.ESSENTIAL,
        color = 0xFF2196F3.toInt(),
        endDate = LocalDate.of(2028, 1, 15),
        isActive = false,
    )

    @Test
    fun roundTrip_full() {
        assertEquals(full, Subscription.parse(full.json))
    }

    @Test
    fun roundTrip_minimal() {
        assertEquals(minimal, Subscription.parse(minimal.json))
    }

    @Test
    fun roundTrip_throughString() {
        // Проверяем и через текст — так данные реально лежат в файле
        assertEquals(full, Subscription.parse(JSONObject(full.json.toString())))
    }

    @Test
    fun defaults_areNotWritten() {
        val json = minimal.json
        assertFalse(json.has("importance"))
        assertFalse(json.has("color"))
        assertFalse(json.has("endDate"))
        assertFalse(json.has("currency"))
        assertFalse(json.has("period"))
        assertFalse(json.has("isActive"))
        // обязательные поля и uid — пишутся всегда
        assertEquals(setOf("uid", "name", "amount", "nextPaymentDate"), json.keys().asSequence().toSet())
    }

    @Test
    fun nonDefaults_areWritten() {
        val json = full.json
        assertEquals("ESSENTIAL", json.getString("importance"))
        assertEquals(0xFF2196F3.toInt(), json.getInt("color"))
        assertEquals("USD", json.getString("currency"))
        // сумма — строкой, чтобы не терять точность
        assertEquals("9.99", json.getString("amount"))
        assertEquals(LocalDate.of(2028, 1, 15).toEpochDay(), json.getLong("endDate"))
    }

    @Test
    fun parse_withoutName_returnsNull() {
        val json = minimal.json.apply { remove("name") }
        assertNull(Subscription.parse(json))
    }

    @Test
    fun parse_withoutAmountOrDate_returnsNull() {
        assertNull(Subscription.parse(minimal.json.apply { remove("amount") }))
        assertNull(Subscription.parse(minimal.json.apply { remove("nextPaymentDate") }))
    }

    @Test
    fun parse_brokenRequiredFields_returnsNull() {
        assertNull(Subscription.parse(minimal.json.put("name", 42)))
        assertNull(Subscription.parse(minimal.json.put("amount", "много")))
        assertNull(Subscription.parse(minimal.json.put("nextPaymentDate", "завтра")))
        // epochDay за пределами LocalDate — не исключение, а null
        assertNull(Subscription.parse(minimal.json.put("nextPaymentDate", Long.MAX_VALUE)))
    }

    @Test
    fun parse_amountAsNumber_oldFormat() {
        // В старых файлах сумма лежит числом
        assertEquals(BigDecimal("199.0"), Subscription.parse(minimal.json.put("amount", 199.0))?.amount)
        assertEquals(BigDecimal("2.99"), Subscription.parse(minimal.json.put("amount", 2.99))?.amount)
    }

    @Test
    fun parse_amountKeepsPrecision() {
        // 0.1 + 0.2 в Double — 0.30000000000000004, в BigDecimal — ровно 0.3
        val sub = minimal.copy(amount = BigDecimal("0.1") + BigDecimal("0.2"))
        assertEquals(BigDecimal("0.3"), Subscription.parse(JSONObject(sub.json.toString()))?.amount)
    }

    @Test
    fun parse_unknownOrBrokenCurrency_returnsNull() {
        assertNull(Subscription.parse(minimal.json.put("currency", "XYZ")))
        assertNull(Subscription.parse(minimal.json.put("currency", "usd")))
        assertNull(Subscription.parse(minimal.json.put("currency", 840)))
    }

    @Test
    fun color_nullMeansNoLabel_whiteIsKept() {
        // «Без метки» не пишется в файл, а белый — обычный цвет и сохраняется
        assertNull(Subscription.parse(minimal.json)?.color)
        val white = minimal.copy(color = 0xFFFFFFFF.toInt())
        assertEquals(white, Subscription.parse(JSONObject(white.json.toString())))
    }

    @Test
    fun parse_emptyObject_returnsNull() {
        assertNull(Subscription.parse(JSONObject()))
    }

    @Test
    fun parse_brokenOptionalFields_fallBackToDefaults() {
        val json = minimal.json
            .put("importance", "SUPER")
            .put("period", 7)
            .put("color", "red")
            .put("isActive", "yes")
        val parsed = Subscription.parse(json)
        assertEquals(minimal, parsed)
    }

    @Test
    fun parse_withoutOrBlankUid_returnsNull() {
        assertNull(Subscription.parse(minimal.json.apply { remove("uid") }))
        assertNull(Subscription.parse(minimal.json.put("uid", "  ")))
        assertNull(Subscription.parse(minimal.json.put("uid", 123)))
    }

    @Test
    fun nextCycle_shiftsByPeriod() {
        val date = LocalDate.of(2026, 1, 31)
        val monthly = minimal.copy(nextPaymentDate = date)
        assertEquals(LocalDate.of(2026, 2, 28), monthly.nextCycle().nextPaymentDate)
        assertEquals(
            LocalDate.of(2026, 2, 7),
            monthly.copy(period = BillingPeriod.WEEKLY).nextCycle().nextPaymentDate,
        )
        assertEquals(
            LocalDate.of(2027, 1, 31),
            monthly.copy(period = BillingPeriod.YEARLY).nextCycle().nextPaymentDate,
        )
    }

    @Test
    fun rolledForward_skipsPastPayments() {
        val sub = minimal.copy(nextPaymentDate = LocalDate.of(2026, 1, 10))
        assertEquals(LocalDate.of(2026, 4, 10), sub.rolledForward(LocalDate.of(2026, 3, 11)).nextPaymentDate)
        // списание сегодня — не прошедшее
        assertEquals(sub, sub.rolledForward(LocalDate.of(2026, 1, 10)))
    }
}
