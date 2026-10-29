package ru.mihendy.subtracker.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val today = LocalDate.of(2026, 10, 18)

    private fun file(): File = File(tmp.root, "subscriptions.json")

    private fun sub(name: String, endDate: LocalDate? = null) = Subscription(
        name = name,
        amount = BigDecimal("100"),
        nextPaymentDate = today.plusDays(5),
        importance = Importance.NORMAL,
        endDate = endDate,
    )

    @Test
    fun saveAndLoad_restoresItems() {
        val storage = SubscriptionStorage(file())
        val a = sub("A")
        val b = sub("B").copy(importance = Importance.OPTIONAL, color = 0xFF00FF00.toInt())
        storage.add(a)
        storage.add(b)
        assertTrue(storage.save())

        val loaded = SubscriptionStorage(file())
        assertEquals(0, loaded.load(today))
        assertEquals(listOf(a, b), loaded.items)
    }

    @Test
    fun save_writeError_returnsFalseAndKeepsItems() {
        // На месте файла лежит папка — writeText бросит IOException
        val dir = tmp.newFolder("subscriptions.json")
        val storage = SubscriptionStorage(dir)
        val a = sub("A")
        storage.add(a)

        assertFalse(storage.save())
        assertEquals(listOf(a), storage.items)
    }

    @Test
    fun load_missingFile_givesEmptyList() {
        val storage = SubscriptionStorage(File(tmp.root, "nope.json"))
        assertEquals(0, storage.load(today))
        assertTrue(storage.items.isEmpty())
    }

    @Test
    fun load_unreadableFile_givesEmptyList() {
        file().writeText("это не json {")
        val storage = SubscriptionStorage(file())
        storage.add(sub("старое"))
        assertEquals(0, storage.load(today))
        assertTrue(storage.items.isEmpty())
    }

    @Test
    fun load_skipsBrokenItems() {
        val good = sub("Хорошая")
        val array = JSONArray()
            .put(good.json)
            .put(good.json.apply { remove("name"); put("uid", "x") })
            .put("строка вместо объекта")
            .put(42)
        file().writeText(array.toString())

        val storage = SubscriptionStorage(file())
        assertEquals(3, storage.load(today))
        assertEquals(listOf(good), storage.items)
    }

    @Test
    fun add_sameUid_replaces() {
        val storage = SubscriptionStorage(file())
        val original = sub("Старое имя")
        storage.add(original)
        storage.add(sub("Другая"))
        storage.add(original.copy(name = "Новое имя"))

        assertEquals(2, storage.items.size)
        assertEquals("Новое имя", storage.items[0].name)
    }

    @Test
    fun remove_existingAndMissingUid() {
        val storage = SubscriptionStorage(file())
        val a = sub("A")
        storage.add(a)
        assertFalse(storage.remove(SubscriptionId("нет-такого-uid")))
        assertEquals(1, storage.items.size)
        assertTrue(storage.remove(a.uid))
        assertTrue(storage.items.isEmpty())
    }

    @Test
    fun items_isSnapshot() {
        val storage = SubscriptionStorage(file())
        val snapshot = storage.items
        storage.add(sub("A"))
        assertTrue(snapshot.isEmpty())
        assertEquals(1, storage.items.size)
    }

    @Test
    fun purgeExpired_removesOnlyPastEndDate() {
        val storage = SubscriptionStorage(file())
        val expired = sub("Истекла", endDate = today.minusDays(1))
        val endsToday = sub("Сегодня", endDate = today)
        val future = sub("Будущая", endDate = today.plusDays(1))
        val endless = sub("Бессрочная")
        listOf(expired, endsToday, future, endless).forEach(storage::add)

        assertEquals(1, storage.purgeExpired(today))
        assertEquals(listOf(endsToday, future, endless), storage.items)
    }

    @Test
    fun load_purgesExpired() {
        val storage = SubscriptionStorage(file())
        val alive = sub("Живая")
        storage.add(alive)
        storage.add(sub("Истекла", endDate = today.minusDays(10)))
        storage.save()

        val loaded = SubscriptionStorage(file())
        // просроченная не считается битой
        assertEquals(0, loaded.load(today))
        assertEquals(listOf(alive), loaded.items)
    }
}
