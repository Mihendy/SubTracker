package ru.mihendy.subtracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber
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
 *
 * Все действия логируются через Timber (в тестах деревьев нет — логи никуда не идут).
 */
class SubscriptionStorage(private val file: File) {

    /** Хранилище в стандартном файле `filesDir/subscriptions.json` приложения. */
    constructor(context: Context) : this(File(context.filesDir, FILE_NAME))

    private val _items = mutableListOf<Subscription>()

    /** Снимок текущих подписок (копия, только для чтения). */
    val items: List<Subscription>
        get() = _items.toList()

    /** Добавляет подписку; если подписка с таким `uid` уже есть — заменяет её. */
    fun add(item: Subscription) {
        if (put(item)) {
            Timber.d("Подписка обновлена: %s (uid=%s)", item.name, item.uid)
        } else {
            Timber.d("Подписка добавлена: %s (uid=%s)", item.name, item.uid)
        }
    }

    /**
     * Кладёт подписку в список без логирования (нужно [load], чтобы не писать
     * в лог каждую прочитанную запись). Возвращает `true`, если заменили существующую.
     */
    private fun put(item: Subscription): Boolean {
        val index = _items.indexOfFirst { it.uid == item.uid }
        if (index >= 0) {
            _items[index] = item
            return true
        }
        _items.add(item)
        return false
    }

    /** Удаляет подписку по [uid]. Возвращает `false`, если такой подписки нет. */
    fun remove(uid: String): Boolean {
        val removed = _items.removeAll { it.uid == uid }
        if (removed) {
            Timber.d("Подписка удалена: uid=%s", uid)
        } else {
            Timber.w("Удаление: подписки с uid=%s нет", uid)
        }
        return removed
    }

    /**
     * Удаляет подписки, у которых [Subscription.endDate] уже прошла (строго раньше [today]).
     * Подписка, заканчивающаяся сегодня, остаётся. Возвращает число удалённых.
     */
    fun purgeExpired(today: LocalDate = LocalDate.now()): Int {
        val before = _items.size
        _items.removeAll { item -> item.endDate?.isBefore(today) ?: false }
        val purged = before - _items.size
        if (purged > 0) Timber.i("Удалено просроченных подписок: %d", purged)
        return purged
    }

    /** Записывает все подписки в файл JSON-массивом. */
    fun save() {
        val array = JSONArray()
        _items.forEach { array.put(it.json) }
        file.parentFile?.mkdirs()
        file.writeText(array.toString())
        Timber.i("Сохранено подписок: %d в %s", _items.size, file.path)
    }

    /**
     * Загружает подписки из файла, заменяя текущие, и удаляет просроченные.
     *
     * @param today дата для [purgeExpired]; параметр нужен для тестов.
     * @return число пропущенных битых записей.
     */
    fun load(today: LocalDate = LocalDate.now()): Int {
        _items.clear()
        if (!file.exists()) {
            Timber.i("Файла %s нет — начинаем с пустого списка", file.path)
            return 0
        }

        val array = try {
            JSONArray(file.readText())
        } catch (e: IOException) {
            Timber.e(e, "Не удалось прочитать %s", file.path)
            return 0
        } catch (e: JSONException) {
            Timber.e(e, "В %s не JSON-массив", file.path)
            return 0
        }

        var skipped = 0
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)?.let { Subscription.parse(it) }
            if (item != null) put(item) else skipped++
        }
        if (skipped > 0) Timber.w("Пропущено битых записей: %d", skipped)
        purgeExpired(today)
        Timber.i("Загружено подписок: %d из %s", _items.size, file.path)
        return skipped
    }

    private companion object {
        const val FILE_NAME = "subscriptions.json"
    }
}
