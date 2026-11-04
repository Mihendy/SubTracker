package ru.mihendy.subtracker.ui.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
 * @param loadInitial загрузка хранилища; возвращает подписку для редактирования
 *   или `null`, если подписок нет (тогда открывается пустая форма).
 * @param save сохранение подписки; `true` — успешно.
 */
@Composable
fun EditSubscriptionRoute(
    loadInitial: suspend () -> Subscription?,
    save: suspend (Subscription) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(stateSaver = SubscriptionDraft.Saver) {
        mutableStateOf(null)
    }

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
                    val ok = save(subscription)
                    val message = if (ok) R.string.edit_saved else R.string.edit_save_failed
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = modifier,
        )
    }
}
