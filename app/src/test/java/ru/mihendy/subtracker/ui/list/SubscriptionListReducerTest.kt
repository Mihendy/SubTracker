package ru.mihendy.subtracker.ui.list

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Money
import ru.mihendy.subtracker.data.Subscription
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionListReducerTest {
    private val subscription = Subscription(
        name = "Музыка",
        price = Money(BigDecimal("199"), Money.RUB),
        nextPaymentDate = LocalDate.of(2026, 12, 1),
        importance = Importance.NORMAL,
    )

    @Test
    fun confirmDeleteRequestsMutation() {
        val state = SubscriptionListState(items = listOf(subscription), confirming = subscription)
        val result = reduceSubscriptionList(state, SubscriptionListAction.ConfirmDelete)

        assertEquals(state.copy(confirming = null, mutation = result.mutation), result)
        assertEquals(subscription, (result.mutation as ListMutation.Delete).subscription)
    }

    @Test
    fun lateResponseCannotReplaceNewerList() = runBlocking {
        val store = SubscriptionListStore()
        val firstId = store.state.value.loadId
        val first = CompletableDeferred<List<Subscription>>()
        val second = CompletableDeferred<List<Subscription>>()
        val firstResponse = launch {
            store.dispatch(SubscriptionListAction.Loaded(firstId, first.await()))
        }
        store.dispatch(SubscriptionListAction.Reload)
        val secondId = store.state.value.loadId
        val secondResponse = launch {
            store.dispatch(SubscriptionListAction.Loaded(secondId, second.await()))
        }

        second.complete(listOf(subscription))
        secondResponse.join()
        val newest = store.state.value
        assertEquals(listOf(subscription), newest.items)

        first.complete(emptyList())
        firstResponse.join()
        assertEquals(newest, store.state.value)
    }
}
