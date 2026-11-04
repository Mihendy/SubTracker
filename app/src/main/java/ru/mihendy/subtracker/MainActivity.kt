package ru.mihendy.subtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.mihendy.subtracker.ui.navigation.SubTrackerNavHost
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme

/**
 * Единственная Activity приложения. Экраны — не отдельные Activity, а пункты
 * навигации внутри неё (см. SubTrackerNavHost).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen: вызывается до super.onCreate, после показа заставки
        // библиотека переключает тему на postSplashScreenTheme
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Репозиторий берём у Application: он один на всё приложение и переживает поворот
        val repository = (application as SubTrackerApp).repository
        setContent {
            SubTrackerTheme {
                SubTrackerNavHost(repository = repository)
            }
        }
    }
}
