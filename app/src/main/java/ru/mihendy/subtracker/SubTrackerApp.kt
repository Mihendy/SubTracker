package ru.mihendy.subtracker

import android.app.Application
import ru.mihendy.subtracker.data.SubscriptionRepository
import ru.mihendy.subtracker.data.SubscriptionStorage
import timber.log.Timber

/**
 * Класс приложения: создаётся системой один раз, раньше любой Activity.
 *
 * Здесь настраивается логирование. Timber сам ничего не выводит — логи уходят
 * в «деревья» (Tree), которые мы «сажаем». DebugTree пишет в Logcat и в качестве
 * тега подставляет имя класса, откуда вызван лог. В release-сборке дерево
 * не сажаем, поэтому все вызовы Timber там ничего не делают.
 *
 * Здесь же живёт [repository]: Application существует, пока жив процесс, поэтому
 * репозиторий один на всё приложение и не пересоздаётся при повороте экрана
 * (Activity при повороте пересоздаётся).
 */
class SubTrackerApp : Application() {

    /** Хранилище подписок. `lazy` — создаётся при первом обращении, а не при старте. */
    val repository by lazy { SubscriptionRepository(SubscriptionStorage(this)) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
