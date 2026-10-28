package ru.mihendy.subtracker.ui.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.mihendy.subtracker.data.BillingPeriod
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Subscription
import java.time.LocalDate

class SubscriptionDraftTest {

    private val date = LocalDate.of(2026, 11, 15)

    private fun valid() = SubscriptionDraft(
        name = "Музыка",
        amountText = "199",
        nextPaymentDate = date,
    )

    @Test
    fun amount_acceptsCommaAndDot() {
        assertEquals(12.5, valid().copy(amountText = "12,5").amount)
        assertEquals(12.5, valid().copy(amountText = " 12.5 ").amount)
    }

    @Test
    fun amount_rejectsGarbageZeroAndNegative() {
        listOf("", "абв", "0", "-5", "1..2", "NaN", "Infinity").forEach { text ->
            assertNull(text, valid().copy(amountText = text).amount)
        }
    }

    @Test
    fun toSubscription_invalidFields_givesNull() {
        assertNull(valid().copy(name = "   ").toSubscription())
        assertNull(valid().copy(amountText = "").toSubscription())
        assertNull(valid().copy(currency = "RU").toSubscription())
        assertNull(valid().copy(currency = "rub").toSubscription())
    }

    @Test
    fun toSubscription_trimsNameAndKeepsFields() {
        val draft = valid().copy(
            name = "  Облако  ",
            amountText = "2.99",
            currency = "USD",
            period = BillingPeriod.YEARLY,
            importance = Importance.ESSENTIAL,
            color = 0xFFC8E6C9.toInt(),
            endDate = date.plusYears(1),
            isActive = false,
        )
        val expected = Subscription(
            uid = draft.uid,
            name = "Облако",
            amount = 2.99,
            currency = "USD",
            period = BillingPeriod.YEARLY,
            nextPaymentDate = date,
            importance = Importance.ESSENTIAL,
            color = 0xFFC8E6C9.toInt(),
            endDate = date.plusYears(1),
            isActive = false,
        )
        assertEquals(expected, draft.toSubscription())
    }

    @Test
    fun from_thenToSubscription_givesSameSubscription() {
        val subscription = Subscription(
            name = "Музыка",
            amount = 199.0,
            nextPaymentDate = date,
            importance = Importance.NORMAL,
        )
        val draft = SubscriptionDraft.from(subscription)
        assertEquals("199", draft.amountText)
        assertEquals(subscription, draft.toSubscription())
    }

    @Test
    fun newDrafts_haveDifferentUids() {
        assertNotEquals(
            SubscriptionDraft(nextPaymentDate = date).uid,
            SubscriptionDraft(nextPaymentDate = date).uid,
        )
    }
}
