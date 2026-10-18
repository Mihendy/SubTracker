package ru.mihendy.subtracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.time.LocalDate

/**
 * Хранилище подписок в JSON-файле.
 *
 * Данные держатся в памяти; [save] записывает их в файл, [load] — читает обратно.
 *
 * Граничные случаи:
 * - [add] с уже существующим `uid` заменяет запись на том же месте списка (порядок не меняется);
 * - [remove] несуществующего `uid` ничего не делает и возвращает `false`;
 * - [items] отдаёт копию списка: изменить хранилище снаружи нельзя, а сам снимок
 *   не меняется при последующих add/remove;
 * - [load] полностью заменяет содержимое памяти данными из файла:
 *   - файла нет → пустой список, это не ошибка (первый запуск);
 *   - файл не читается или это не JSON-массив → пустой список, возвращается 0;
 *   - отдельные битые элементы пропускаются, их число возвращается;
 *   - повторяющиеся `uid` в файле — остаётся последняя запись (как при [add]);
 *   - после чтения вызывается [purgeExpired], удалённые просроченные подписки
 *     битыми не считаются;
 * - [save] ошибки записи (нет места, нет прав) не глотает — бросает [IOException].
 */
class SubscriptionStorage(private val file: File) {

    /** Хранилище в стандартном файле `filesDir/subscriptions.json` приложения. */
    @Suppress("unused") // будет использоваться UI-слоем в следующих заданиях
    constructor(context: Context) : this(File(context.filesDir, FILE_NAME))

    private val _items = mutableListOf<Subscription>()

    /** Снимок текущих подписок (копия, только для чтения). */
    val items: List<Subscription>
        get() = _items.toList()

    /** Добавляет подписку; если подписка с таким `uid` уже есть — заменяет её. */
    fun add(item: Subscription) {
        val index = _items.indexOfFirst { it.uid == item.uid }
        if (index >= 0) {
            _items[index] = item
        } else {
            _items.add(item)
        }
    }

    /** Удаляет подписку по [uid]. Возвращает `false`, если такой подписки нет. */
    fun remove(uid: String): Boolean = _items.removeAll { it.uid == uid }

    /**
     * Удаляет подписки, у которых [Subscription.endDate] уже прошла (строго раньше [today]).
     * Подписка, заканчивающаяся сегодня, остаётся. Возвращает число удалённых.
     */
    fun purgeExpired(today: LocalDate = LocalDate.now()): Int {
        val before = _items.size
        _items.removeAll { item -> item.endDate?.isBefore(today) ?: false }
        return before - _items.size
    }

    /** Записывает все подписки в файл JSON-массивом. */
    fun save() {
        val array = JSONArray()
        _items.forEach { array.put(it.json) }
        file.parentFile?.mkdirs()
        file.writeText(array.toString())
    }

    /**
     * Загружает подписки из файла, заменяя текущие, и удаляет просроченные.
     *
     * @param today дата для [purgeExpired]; параметр нужен для тестов.
     * @return число пропущенных битых записей.
     */
    fun load(today: LocalDate = LocalDate.now()): Int {
        _items.clear()
        if (!file.exists()) return 0

        val array = try {
            JSONArray(file.readText())
        } catch (e: IOException) {
            return 0
        } catch (e: JSONException) {
            return 0
        }

        var skipped = 0
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)?.let { Subscription.parse(it) }
            if (item != null) add(item) else skipped++
        }
        purgeExpired(today)
        return skipped
    }

    private companion object {
        const val FILE_NAME = "subscriptions.json"
    }
}
