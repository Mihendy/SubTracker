package ru.mihendy.subtracker.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mihendy.subtracker.data.SubscriptionId
import ru.mihendy.subtracker.data.SubscriptionRepository
import ru.mihendy.subtracker.ui.edit.EditSubscriptionRoute
import ru.mihendy.subtracker.ui.list.SubscriptionListRoute

/**
 * Маршруты приложения — строки, по которым NavHost находит экран.
 *
 * Экран редактирования один и для новой, и для существующей подписки:
 * `edit?subscriptionId=…` — редактировать эту, просто `edit` — создать новую.
 * Аргумент необязательный (`?`), без него `subscriptionId` будет `null`.
 *
 * Между экранами передаётся только id, а не подписка целиком: маршрут — это строка,
 * и сама подписка в неё не влезет; к тому же экран редактирования возьмёт
 * актуальную версию из хранилища, а не устаревшую копию.
 */
private object Routes {
    const val LIST = "subscriptions"
    const val ARG_ID = "subscriptionId"
    const val EDIT = "edit?$ARG_ID={$ARG_ID}"

    /** Маршрут на редактирование подписки [id]; `null` — новая подписка. */
    fun edit(id: SubscriptionId?): String =
        // Uri.encode — на случай, если в id окажутся символы, особые для URL (?, &, /)
        if (id == null) "edit" else "edit?$ARG_ID=${Uri.encode(id.value)}"
}

/**
 * Граф навигации: список подписок → экран редактирования.
 *
 * NavController создаётся здесь один раз, а экраны о нём не знают: им передаются
 * лямбды «открой то-то», «закройся». Так экраны можно показать в @Preview и
 * переиспользовать, а вся логика переходов собрана в одном месте.
 */
@Composable
fun SubTrackerNavHost(
    repository: SubscriptionRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
        modifier = modifier,
    ) {
        composable(Routes.LIST) {
            // collectAsState: при каждом новом списке в репозитории экран перерисуется
            val items by repository.items.collectAsState()
            LaunchedEffect(Unit) { repository.load() }

            SubscriptionListRoute(
                items = items,
                delete = repository::delete,
                restore = repository::save,
                onOpen = { id -> navController.openEditor(id) },
                onAdd = { navController.openEditor(null) },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Routes.ARG_ID)
                ?.takeIf { it.isNotBlank() }
                ?.let(::SubscriptionId)

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                EditSubscriptionRoute(
                    loadInitial = { id?.let { repository.get(it) } },
                    save = repository::save,
                    // suspend { } — явно говорим, что лямбда приостанавливаемая
                    delete = if (id == null) null else suspend { repository.delete(id) },
                    // popBackStack до списка, а не просто «назад на один шаг»: если
                    // «Сохранить» нажали дважды, второй вызов ничего не сделает,
                    // вместо того чтобы закрыть ещё и список вместе с приложением
                    onDone = { navController.popBackStack(Routes.LIST, inclusive = false) },
                    // consumeWindowInsets: отступ под системные панели уже учтён,
                    // и imePadding внутри экрана не добавит его второй раз
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }
    }
}

/**
 * Открывает экран редактирования.
 * launchSingleTop: если редактор уже сверху (двойное нажатие на «+»),
 * второй такой же экран не открывается.
 */
private fun NavHostController.openEditor(id: SubscriptionId?) {
    navigate(Routes.edit(id)) { launchSingleTop = true }
}
