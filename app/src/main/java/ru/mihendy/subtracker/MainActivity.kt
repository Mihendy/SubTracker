package ru.mihendy.subtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.data.SubscriptionStorage
import ru.mihendy.subtracker.ui.edit.EditSubscriptionRoute
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val storage by lazy { SubscriptionStorage(applicationContext) }

    /**
     * Поток для работы с файлом: не главный (Dispatchers.IO), и задачи в нём
     * выполняются строго по одной. SubscriptionStorage не потокобезопасен,
     * а так загрузка и сохранение никогда не пойдут одновременно.
     */
    private val storageDispatcher = Dispatchers.IO.limitedParallelism(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen: вызывается до super.onCreate, после показа заставки
        // библиотека переключает тему на postSplashScreenTheme
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EditSubscriptionRoute(
                        loadInitial = ::loadFirstSubscription,
                        save = ::saveSubscription,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    /**
     * Читает файл и возвращает первую подписку для редактирования.
     * Пока на экране одна форма без списка (список — в следующих заданиях).
     */
    private suspend fun loadFirstSubscription(): Subscription? = withContext(storageDispatcher) {
        storage.load()
        storage.items.firstOrNull()
    }

    /** Добавляет или обновляет подписку (по uid) и записывает файл. */
    private suspend fun saveSubscription(subscription: Subscription): Boolean =
        withContext(storageDispatcher) {
            storage.add(subscription)
            storage.save()
        }.also { ok ->
            if (!ok) Timber.w("Подписку %s сохранить не удалось", subscription.uid)
        }
}
