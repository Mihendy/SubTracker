package ru.mihendy.subtracker.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(): File = File(tmp.root, "subscriptions.json")

    /** Новый репозиторий над тем же файлом — как после перезапуска приложения. */
    private fun repository() = SubscriptionRepository(SubscriptionStorage(file()))

    private fun sub(name: String) = Subscription(
        name = name,
        price = Money(BigDecimal("100"), Money.RUB),
        // Дата в будущем, чтобы load() не счёл подписку просроченной
        nextPaymentDate = LocalDate.now().plusDays(5),
        importance = Importance.NORMAL,
    )

    @Test
    fun items_nullUntilLoaded() = runBlocking {
        val repository = repository()
        assertNull(repository.items.value)
        repository.load()
        assertEquals(emptyList<Subscription>(), repository.items.value)
    }

    @Test
    fun save_updatesItemsAndFile() = runBlocking {
        val a = sub("A")
        val repository = repository()
        assertTrue(repository.save(a))
        assertEquals(listOf(a), repository.items.value)

        // Новый репозиторий читает то же самое из файла
        val reopened = repository()
        reopened.load()
        assertEquals(listOf(a), reopened.items.value)
    }

    @Test
    fun save_beforeLoad_keepsExistingSubscriptions() = runBlocking {
        val a = sub("A")
        repository().save(a)

        // Сохраняем, не вызвав load(): старая подписка из файла не должна пропасть
        val b = sub("B")
        val repository = repository()
        repository.save(b)
        assertEquals(listOf(a, b), repository.items.value)
    }

    @Test
    fun delete_removesFromItemsAndFile() = runBlocking {
        val a = sub("A")
        val b = sub("B")
        val repository = repository()
        repository.save(a)
        repository.save(b)

        assertTrue(repository.delete(a.uid))
        assertEquals(listOf(b), repository.items.value)

        val reopened = repository()
        reopened.load()
        assertEquals(listOf(b), reopened.items.value)
    }

    @Test
    fun delete_unknownId_returnsFalse() = runBlocking {
        val repository = repository()
        repository.save(sub("A"))
        assertFalse(repository.delete(SubscriptionId("нет-такого")))
        assertEquals(1, repository.items.value?.size)
    }

    @Test
    fun deleteThenSave_restoresSameSubscription() = runBlocking {
        // Так работает «Отменить» после свайпа: та же подписка с тем же uid
        val a = sub("A")
        val repository = repository()
        repository.save(a)
        repository.delete(a.uid)
        repository.save(a)
        assertEquals(listOf(a), repository.items.value)
    }

    @Test
    fun get_findsById_loadingFileIfNeeded() = runBlocking {
        val a = sub("A")
        repository().save(a)

        val repository = repository()
        assertEquals(a, repository.get(a.uid))
        assertNull(repository.get(SubscriptionId("нет-такого")))
    }

    @Test
    fun save_pastDate_isRolledForward() = runBlocking {
        // В форме выбрали прошлую дату — в список попадает уже следующее списание
        val past = sub("A").copy(nextPaymentDate = LocalDate.now().minusDays(10))
        val repository = repository()
        repository.save(past)
        val saved = repository.items.value?.single()?.nextPaymentDate
        assertEquals(past.rolledForward(LocalDate.now()).nextPaymentDate, saved)
        assertFalse(saved?.isBefore(LocalDate.now()) ?: true)
    }
}
