package ru.mihendy.subtracker.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate

/**
 * Единая точка доступа к подпискам для экранов.
 *
 * Оборачивает [SubscriptionStorage]: сам ходит в файл в фоновом потоке и держит
 * актуальный список в [items]. Экраны только подписываются на [items] и вызывают
 * методы — после каждого изменения список обновляется сам, и список на экране
 * перерисовывается без ручного «перезагрузи».
 *
 * Создаётся один раз на всё приложение (см. SubTrackerApp), поэтому переживает
 * поворот экрана: файл не перечитывается при каждом пересоздании Activity.
 *
 * @param dispatcher поток для работы с файлом. По умолчанию — Dispatchers.IO,
 *   но задачи выполняются строго по одной: [SubscriptionStorage] не потокобезопасен,
 *   а так загрузка, сохранение и удаление никогда не пойдут одновременно.
 */
class SubscriptionRepository(
    private val storage: SubscriptionStorage,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) {

    private val _items = MutableStateFlow<List<Subscription>?>(null)

    /** Текущие подписки; `null` — файл ещё не прочитан. */
    val items: StateFlow<List<Subscription>?> = _items.asStateFlow()

    /** Читает файл, если он ещё не прочитан. Повторные вызовы ничего не делают. */
    suspend fun load() = withContext(dispatcher) { loadIfNeeded() }

    /** Подписка по [id] или `null`, если такой нет. При необходимости сначала читает файл. */
    suspend fun get(id: SubscriptionId): Subscription? {
        load()
        return _items.value?.firstOrNull { it.uid == id }
    }

    /**
     * Добавляет или обновляет подписку (по uid) и записывает файл.
     *
     * Если дата списания уже прошла (в форме выбрали прошлое число), она сдвигается
     * вперёд на целое число периодов — так же, как при чтении файла.
     *
     * @return `true`, если файл записан.
     */
    suspend fun save(subscription: Subscription): Boolean = withContext(dispatcher) {
        loadIfNeeded()
        storage.add(subscription.rolledForward(LocalDate.now()))
        commit().also { ok ->
            if (!ok) Timber.w("Подписку %s сохранить не удалось", subscription.uid)
        }
    }

    /**
     * Удаляет подписку и записывает файл.
     * @return `true`, если подписка была и файл записан.
     */
    suspend fun delete(id: SubscriptionId): Boolean = withContext(dispatcher) {
        loadIfNeeded()
        storage.remove(id) && commit()
    }

    /**
     * Читает файл, если ещё не читали. Вызывается перед любым изменением: иначе
     * [save] до [load] записал бы в файл одну новую подписку и стёр остальные.
     * Вызывать только внутри `withContext(dispatcher)`.
     */
    private fun loadIfNeeded() {
        if (_items.value != null) return
        storage.load()
        _items.value = storage.items
    }

    /**
     * Записывает файл и публикует новый список в [items].
     * Список публикуется даже при ошибке записи: в памяти изменение уже есть,
     * и экран должен показывать то же, что лежит в хранилище.
     */
    private fun commit(): Boolean {
        val ok = storage.save()
        _items.value = storage.items
        return ok
    }
}
