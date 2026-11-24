package ru.mihendy.subtracker.ui.list

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.data.SubscriptionId

@Composable
fun SubscriptionListRoute(
    store: SubscriptionListStore,
    onOpen: (SubscriptionId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by store.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    val editor = state.editor
    LaunchedEffect(editor) {
        if (editor != null) {
            onOpen(editor.id)
            store.dispatch(SubscriptionListAction.EditorOpened(editor))
        }
    }

    val notice = state.notice
    LaunchedEffect(notice, resources) {
        if (notice == null) return@LaunchedEffect
        val result = when (notice) {
            is ListNotice.Deleted -> snackbar.showSnackbar(
                message = resources.getString(R.string.list_deleted, notice.subscription.name),
                actionLabel = resources.getString(R.string.list_undo),
                duration = SnackbarDuration.Short,
            )
            is ListNotice.DeleteFailed -> snackbar.showSnackbar(
                resources.getString(R.string.edit_delete_failed),
            )
            is ListNotice.RestoreFailed -> snackbar.showSnackbar(
                resources.getString(R.string.edit_save_failed),
            )
        }
        store.dispatch(
            SubscriptionListAction.NoticeClosed(notice, result == SnackbarResult.ActionPerformed),
        )
    }

    SubscriptionListScreen(
        state = state,
        snackbarHostState = snackbar,
        onAction = store::dispatch,
        modifier = modifier,
    )
}
