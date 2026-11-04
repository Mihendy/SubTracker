package ru.mihendy.subtracker.ui.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.data.Subscription
import java.time.LocalDate

/**
 * Владелец состояния экрана редактирования: хранит черновик и связывает
 * stateless-[EditSubscriptionScreen] с хранилищем.
 *
 * Черновик лежит в [rememberSaveable], поэтому введённое, но не сохранённое
 * переживает поворот экрана. `null` — данные ещё не загружены.
 *
 * Экран не знает про навигацию: закрыть себя он просит через [onDone],
 * а куда при этом вернуться — решает вызывающий код.
 *
 * @param loadInitial возвращает подписку для редактирования или `null` —
 *   тогда открывается пустая форма новой подписки.
 * @param save сохранение подписки; `true` — успешно.
 * @param delete удаление этой подписки; `true` — успешно. `null` — подписка новая,
 *   удалять нечего, кнопки «Удалить» нет.
 * @param onDone подписка сохранена или удалена — экран можно закрывать.
 */
@Composable
fun EditSubscriptionRoute(
    loadInitial: suspend () -> Subscription?,
    save: suspend (Subscription) -> Boolean,
    delete: (suspend () -> Boolean)?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(stateSaver = SubscriptionDraft.Saver) {
        mutableStateOf(null)
    }
    // Открыт ли диалог «Удалить?»; rememberSaveable — чтобы он не пропал при повороте
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Загружаем хранилище при каждом создании экрана (в том числе после поворота),
    // но черновик заполняем, только если он не восстановился из rememberSaveable
    LaunchedEffect(Unit) {
        val initial = loadInitial()
        if (draft == null) {
            draft = initial?.let { SubscriptionDraft.from(it) }
                ?: SubscriptionDraft(nextPaymentDate = LocalDate.now())
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val current = draft

    if (current == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        EditSubscriptionScreen(
            draft = current,
            onDraftChange = { draft = it },
            onSave = {
                // Кнопка активна только для валидного черновика, но проверяем ещё раз
                val subscription = current.toSubscription() ?: return@EditSubscriptionScreen
                scope.launch {
                    if (save(subscription)) {
                        onDone()
                    } else {
                        Toast.makeText(context, R.string.edit_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = delete?.let { { confirmDelete = true } },
            modifier = modifier,
        )
    }

    if (confirmDelete && delete != null && current != null) {
        DeleteConfirmDialog(
            name = current.name,
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    if (delete()) {
                        onDone()
                    } else {
                        Toast.makeText(context, R.string.edit_delete_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Диалог «Удалить подписку «…»?» с кнопками «Удалить» и «Отмена». */
@Composable
private fun DeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_delete_confirm, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.edit_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
