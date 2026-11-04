package ru.mihendy.subtracker.ui.list

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.data.SubscriptionId

/**
 * Владелец состояния экрана списка: связывает stateless-[SubscriptionListScreen]
 * с хранилищем и показывает «Удалено — Отменить».
 *
 * Удаление срабатывает сразу, а snackbar даёт его отменить: «Отменить» сохраняет
 * подписку обратно (с тем же uid, поэтому это та же самая подписка, а не копия).
 *
 * @param items подписки; `null` — ещё загружаются.
 * @param delete удаление по id; `true` — удалено и записано в файл.
 * @param restore сохранение удалённой подписки обратно (для «Отменить»).
 * @param onOpen открыть подписку на редактирование.
 * @param onAdd открыть форму новой подписки.
 */
@Composable
fun SubscriptionListRoute(
    items: List<Subscription>?,
    delete: suspend (SubscriptionId) -> Boolean,
    restore: suspend (Subscription) -> Boolean,
    onOpen: (SubscriptionId) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // LocalResources, а не LocalContext.current.getString: при смене конфигурации
    // (например, языка) Compose перечитает строки, а не покажет устаревшие
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.list_undo)

    SubscriptionListScreen(
        items = items,
        snackbarHostState = snackbarHostState,
        onItemClick = onOpen,
        onAddClick = onAdd,
        onDelete = { subscription ->
            scope.launch {
                // Прошлое «Удалено — Отменить» убираем сразу, а не ждём, пока исчезнет само
                snackbarHostState.currentSnackbarData?.dismiss()
                if (!delete(subscription.uid)) {
                    snackbarHostState.showSnackbar(resources.getString(R.string.edit_delete_failed))
                    return@launch
                }
                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.list_deleted, subscription.name),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) restore(subscription)
            }
        },
        modifier = modifier,
    )
}
