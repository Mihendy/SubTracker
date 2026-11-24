package ru.mihendy.subtracker.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.data.BillingPeriod
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.data.Money
import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.ui.common.SubscriptionCard
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

/*
 * Экран списка подписок. Как и экран редактирования, stateless: список приходит
 * параметром, нажатия уходят колбэками. Что делать при удалении и куда переходить
 * по нажатию — решает вызывающий код (SubscriptionListRoute и навигация).
 */

/** Отступ снизу списка, чтобы последнюю карточку не закрывала кнопка «+». */
private val FAB_CLEARANCE = 88.dp

/** Рисует список и преобразует жесты в действия. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    state: SubscriptionListState,
    snackbarHostState: SnackbarHostState,
    onAction: (SubscriptionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = state.items
    state.confirming?.let { subscription ->
        AlertDialog(
            onDismissRequest = { onAction(SubscriptionListAction.CancelDelete) },
            text = { Text(stringResource(R.string.edit_delete_confirm, subscription.name)) },
            confirmButton = {
                TextButton(onClick = { onAction(SubscriptionListAction.ConfirmDelete) }) {
                    Text(stringResource(R.string.edit_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SubscriptionListAction.CancelDelete) }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(SubscriptionListAction.Open(null)) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.list_add),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.loadFailed -> Box(contentModifier, contentAlignment = Alignment.Center) {
                TextButton(onClick = { onAction(SubscriptionListAction.Reload) }) {
                    Text(stringResource(R.string.list_retry))
                }
            }

            items == null -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            items.isEmpty() -> Box(contentModifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            else -> SubscriptionList(
                items = items,
                confirming = state.confirming,
                canDelete = state.mutation == null,
                onAction = onAction,
                modifier = contentModifier,
            )
        }
    }
}

/** Сам список: ближайшие списания сверху. */
@Composable
private fun SubscriptionList(
    items: List<Subscription>,
    confirming: Subscription?,
    canDelete: Boolean,
    onAction: (SubscriptionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // remember(items): сортируем заново, только когда пришёл новый список,
    // а не при каждой рекомпозиции
    val sorted = remember(items) { items.sortedBy { it.nextPaymentDate } }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // key — id подписки. Без него Compose различает строки по позиции, и после
        // удаления состояние свайпа «переехало» бы на соседнюю карточку
        items(sorted, key = { it.uid.value }) { subscription ->
            SwipeToDeleteRow(
                subscription = subscription,
                confirming = subscription.uid == confirming?.uid,
                canDelete = canDelete,
                onClick = { onAction(SubscriptionListAction.Open(subscription.uid)) },
                onDismiss = { onAction(SubscriptionListAction.AskToDelete(subscription)) },
                // Плавно сдвигает соседние карточки, когда одна удаляется или возвращается
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/**
 * Карточка, которую можно смахнуть влево для удаления.
 * Под карточкой при этом проступает красная подложка с корзиной.
 */
@Composable
private fun SwipeToDeleteRow(
    subscription: Subscription,
    confirming: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Обычный remember, а не rememberSwipeToDismissBoxState(): тот сохраняет состояние
    // через rememberSaveable, а LazyColumn хранит сохранённое по key строки. Тогда после
    // «Отменить» подписка с тем же uid вернулась бы уже смахнутой и удалилась бы снова
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember {
        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold)
    }
    // Диалог закрыт (отмена или после удаления) — возвращаем карточку на место.
    // Если удаление не удалось, карточка остаётся в списке, а не «уезжает» за край
    LaunchedEffect(confirming) {
        if (!confirming && state.currentValue != SwipeToDismissBoxValue.Settled) state.reset()
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Подложку рисуем только во время свайпа: карточка приостановленной
            // подписки полупрозрачная, и красный просвечивал бы сквозь неё всегда
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                DeleteBackground()
            }
        },
        modifier = modifier,
        // Только справа налево: свайп вправо ничего не делает
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canDelete,
        onDismiss = { onDismiss() },
    ) {
        SubscriptionCard(
            name = subscription.name,
            price = subscription.price,
            nextPaymentDate = subscription.nextPaymentDate,
            color = subscription.color,
            isActive = subscription.isActive,
            // clip — чтобы волна от нажатия не вылезала за скруглённые углы карточки
            modifier = Modifier
                .clip(CardDefaults.shape)
                .clickable(onClick = onClick),
        )
    }
}

/** Красная подложка с корзиной справа. */
@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardDefaults.shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.list_delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

// ---- Превью: список, пустой список, загрузка ----

private val previewItems = listOf(
    Subscription(
        name = "Музыка",
        price = Money(BigDecimal("199"), Money.RUB),
        nextPaymentDate = LocalDate.of(2026, 11, 15),
        importance = Importance.NORMAL,
        color = 0xFF81C784.toInt(),
    ),
    Subscription(
        name = "Облако",
        price = Money(BigDecimal("2.99"), Currency.getInstance("USD")),
        period = BillingPeriod.YEARLY,
        nextPaymentDate = LocalDate.of(2026, 11, 8),
        importance = Importance.ESSENTIAL,
        color = 0xFF4FC3F7.toInt(),
    ),
    Subscription(
        name = "Кинотеатр с очень длинным названием, которое не помещается в одну строку",
        price = Money(BigDecimal("399"), Money.RUB),
        nextPaymentDate = LocalDate.of(2026, 12, 1),
        importance = Importance.OPTIONAL,
        isActive = false,
    ),
)

@Preview(showBackground = true, name = "Список")
@Composable
private fun ListPreview() {
    SubTrackerTheme(dynamicColor = false) {
        SubscriptionListScreen(
            state = SubscriptionListState(items = previewItems),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Пусто")
@Composable
private fun EmptyPreview() {
    SubTrackerTheme(dynamicColor = false) {
        SubscriptionListScreen(
            state = SubscriptionListState(items = emptyList()),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Загрузка")
@Composable
private fun LoadingPreview() {
    SubTrackerTheme(dynamicColor = false) {
        SubscriptionListScreen(
            state = SubscriptionListState(items = null),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
