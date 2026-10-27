package ru.mihendy.subtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.data.SubscriptionStorage
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import timber.log.Timber
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen: вызывается до super.onCreate, после показа заставки
        // библиотека переключает тему на postSplashScreenTheme
        installSplashScreen()
        super.onCreate(savedInstanceState)
        checkStorage()
        enableEdgeToEdge()
        setContent {
            SubTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Временная проверка хранилища (пока нет UI): загрузить, при первом запуске
     * добавить демо-подписки, сохранить и прочитать файл заново. Результат — в Logcat.
     *
     * Работа с файлом идёт в [Dispatchers.IO], а не в главном потоке, чтобы
     * не подвешивать интерфейс. [lifecycleScope] сам отменит корутину,
     * если Activity уничтожат раньше, чем она закончится.
     */
    private fun checkStorage() {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val storage = SubscriptionStorage(applicationContext)
                storage.load()
                if (storage.items.isEmpty()) {
                    demoSubscriptions().forEach { storage.add(it) }
                }
                if (!storage.save()) {
                    Timber.w("Проверка хранилища: сохранить не удалось, читаем то, что есть на диске")
                }

                // Новый экземпляр читает тот же файл — так видно, что данные реально на диске
                SubscriptionStorage(applicationContext).apply { load() }.items
            }
            Timber.i("Проверка хранилища: прочитано %d подписок", loaded.size)
            loaded.forEach { Timber.d("  %s", it) }
        }
    }

    private fun demoSubscriptions(): List<Subscription> {
        val today = LocalDate.now()
        return listOf(
            Subscription(
                name = "Музыка",
                amount = 199.0,
                nextPaymentDate = today.plusDays(1),
                importance = Importance.NORMAL,
            ),
            Subscription(
                name = "Облако",
                amount = 2.99,
                currency = "USD",
                nextPaymentDate = today.plusDays(10),
                importance = Importance.ESSENTIAL,
            ),
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SubTrackerTheme {
        Greeting("Android")
    }
}