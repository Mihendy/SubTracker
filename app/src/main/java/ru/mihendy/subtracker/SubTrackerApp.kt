package ru.mihendy.subtracker

import android.app.Application
import timber.log.Timber

/**
 * Класс приложения: создаётся системой один раз, раньше любой Activity.
 *
 * Здесь настраивается логирование. Timber сам ничего не выводит — логи уходят
 * в «деревья» (Tree), которые мы «сажаем». DebugTree пишет в Logcat и в качестве
 * тега подставляет имя класса, откуда вызван лог. В release-сборке дерево
 * не сажаем, поэтому все вызовы Timber там ничего не делают.
 */
class SubTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
