package ru.mihendy.subtracker.ui.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mihendy.subtracker.R
import ru.mihendy.subtracker.data.BillingPeriod
import ru.mihendy.subtracker.data.Importance
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * Экран создания/редактирования подписки.
 *
 * Все функции здесь stateless: получают значения параметрами и сообщают
 * об изменениях колбэками («состояние вниз, события вверх»). Где хранится
 * черновик — решает вызывающий код (см. EditSubscriptionRoute). Поэтому
 * любую функцию можно показать в @Preview без заглушек.
 */

/** Цвета-метки на выбор (ARGB): белый и приглушённые цвета радуги. «Без метки» — отдельный кружок. */
val SubscriptionColors: List<Int> = listOf(
    0xFFFFFFFF, // белый
    0xFFE57373, // красный
    0xFFFFB74D, // оранжевый
    0xFFFFF176, // жёлтый
    0xFF81C784, // зелёный
    0xFF4FC3F7, // голубой
    0xFF7986CB, // синий
    0xFFBA68C8, // фиолетовый
).map { it.toInt() }

/** Максимальная ширина формы: шире поля ввода читать неудобно. */
private val FORM_MAX_WIDTH = 600.dp

/**
 * Вся форма целиком.
 *
 * @param draft что показывать.
 * @param onDraftChange вызывается с новой копией черновика при любом изменении поля.
 * @param onSave нажата «Сохранить»; кнопка доступна, только если черновик валиден.
 */
@Composable
fun EditSubscriptionScreen(
    draft: SubscriptionDraft,
    onDraftChange: (SubscriptionDraft) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Прокрутка нужна, чтобы в альбомной ориентации форма не обрезалась снизу.
    // На широком экране форма не растягивается больше FORM_MAX_WIDTH и стоит по центру.
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        EditSubscriptionForm(draft, onDraftChange, onSave)
    }
}

/** Поля формы друг под другом. */
@Composable
private fun EditSubscriptionForm(
    draft: SubscriptionDraft,
    onDraftChange: (SubscriptionDraft) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = FORM_MAX_WIDTH)
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.edit_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        NameField(
            name = draft.name,
            isError = !draft.isNameValid,
            onNameChange = { onDraftChange(draft.copy(name = it)) },
        )
        AmountRow(
            amountText = draft.amountText,
            currency = draft.currency,
            isAmountError = draft.amount == null,
            isCurrencyError = !draft.isCurrencyValid,
            onAmountChange = { onDraftChange(draft.copy(amountText = it)) },
            onCurrencyChange = { onDraftChange(draft.copy(currency = it)) },
        )
        ChoiceSection(
            title = stringResource(R.string.edit_period),
            options = BillingPeriod.entries,
            selected = draft.period,
            label = { stringResource(it.labelRes()) },
            onSelect = { onDraftChange(draft.copy(period = it)) },
        )
        DatesSection(
            nextPaymentDate = draft.nextPaymentDate,
            endDate = draft.endDate,
        )
        ChoiceSection(
            title = stringResource(R.string.edit_importance),
            options = Importance.entries,
            selected = draft.importance,
            label = { stringResource(it.labelRes()) },
            onSelect = { onDraftChange(draft.copy(importance = it)) },
        )
        ColorSection(
            colors = SubscriptionColors,
            selected = draft.color,
            onSelect = { onDraftChange(draft.copy(color = it)) },
        )
        ActiveRow(
            isActive = draft.isActive,
            onActiveChange = { onDraftChange(draft.copy(isActive = it)) },
        )
        Button(
            onClick = onSave,
            enabled = draft.toSubscription() != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.edit_save))
        }
    }
}

/** Поле названия; при пустом значении подсвечивается ошибкой. */
@Composable
fun NameField(
    name: String,
    isError: Boolean,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.edit_name)) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.edit_name_error)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Сумма и валюта в одной строке. Сумма занимает всё свободное место,
 * валюта — фиксированную ширину, поэтому строка не ломается на узких и широких экранах.
 */
@Composable
fun AmountRow(
    amountText: String,
    currency: String,
    isAmountError: Boolean,
    isCurrencyError: Boolean,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.edit_amount)) },
            isError = isAmountError,
            supportingText = if (isAmountError) {
                { Text(stringResource(R.string.edit_amount_error)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = currency,
            // Код валюты: только латиница, сразу в верхний регистр, не длиннее трёх букв
            onValueChange = { text ->
                onCurrencyChange(text.filter { it in 'a'..'z' || it in 'A'..'Z' }.uppercase().take(3))
            },
            label = { Text(stringResource(R.string.edit_currency)) },
            isError = isCurrencyError,
            supportingText = if (isCurrencyError) {
                { Text(stringResource(R.string.edit_currency_error)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.width(112.dp),
        )
    }
}

/**
 * Заголовок и выбор одного варианта из небольшого списка (период, важность).
 * Обобщённая функция: работает с любым типом [T], подпись даёт [label].
 */
@Composable
fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(label(option), maxLines = 1)
                }
            }
        }
    }
}

/**
 * Даты списания и окончания. Пока только отображаются:
 * выбор даты через календарь — усложнение, в базовой части его нет.
 */
@Composable
fun DatesSection(
    nextPaymentDate: LocalDate,
    endDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledValue(
            label = stringResource(R.string.edit_next_payment),
            value = nextPaymentDate.format(formatter),
        )
        LabeledValue(
            label = stringResource(R.string.edit_end_date),
            value = endDate?.format(formatter) ?: stringResource(R.string.edit_end_date_none),
        )
    }
}

/** Строка «подпись слева — значение справа». */
@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Выбор цветовой метки: ряд кружков, выбранный обведён рамкой.
 * Первый кружок — перечёркнутый «без метки» (`null`), за ним цвета из [colors].
 * [FlowRow] переносит кружки на новую строку, если они не помещаются по ширине.
 */
@Composable
fun ColorSection(
    colors: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.edit_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorDot(
                color = null,
                isSelected = selected == null,
                description = stringResource(R.string.edit_color_none),
                onClick = { onSelect(null) },
            )
            colors.forEachIndexed { index, color ->
                ColorDot(
                    color = Color(color),
                    isSelected = color == selected,
                    description = stringResource(R.string.edit_color_item, index + 1),
                    onClick = { onSelect(color) },
                )
            }
        }
    }
}

/**
 * Один кружок выбора цвета. Тонкая рамка есть всегда (чтобы был виден белый),
 * у выбранного — толстая. [color] == `null` — «без метки»: пустой кружок с диагональю.
 */
@Composable
private fun ColorDot(
    color: Color?,
    isSelected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val strikeColor = MaterialTheme.colorScheme.outline
    Spacer(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (color != null) {
                    Modifier.background(color)
                } else {
                    // Диагональ из левого нижнего угла в правый верхний
                    Modifier.drawBehind {
                        drawLine(
                            color = strikeColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            )
            .border(border, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isSelected
            },
    )
}

/** Переключатель «Активна». */
@Composable
fun ActiveRow(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.edit_active),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = isActive, onCheckedChange = onActiveChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

// when без else: если в enum добавят вариант, здесь будет ошибка компиляции
@StringRes
private fun BillingPeriod.labelRes(): Int = when (this) {
    BillingPeriod.WEEKLY -> R.string.period_weekly
    BillingPeriod.MONTHLY -> R.string.period_monthly
    BillingPeriod.YEARLY -> R.string.period_yearly
}

@StringRes
private fun Importance.labelRes(): Int = when (this) {
    Importance.OPTIONAL -> R.string.importance_optional
    Importance.NORMAL -> R.string.importance_normal
    Importance.ESSENTIAL -> R.string.importance_essential
}

// ---- Превью: пустая форма, заполненная, длинный текст, выключенный флаг, альбомная ----

private val previewDate = LocalDate.of(2026, 11, 15)

private val filledDraft = SubscriptionDraft(
    name = "Музыка",
    amountText = "199",
    nextPaymentDate = previewDate,
    importance = Importance.ESSENTIAL,
    color = SubscriptionColors[4],
    endDate = previewDate.plusYears(1),
)

@Preview(showBackground = true, name = "Пустая форма")
@Composable
private fun EmptyPreview() {
    SubTrackerTheme(dynamicColor = false) {
        EditSubscriptionScreen(
            draft = SubscriptionDraft(nextPaymentDate = previewDate),
            onDraftChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "Заполненная")
@Composable
private fun FilledPreview() {
    SubTrackerTheme(dynamicColor = false) {
        EditSubscriptionScreen(draft = filledDraft, onDraftChange = {}, onSave = {})
    }
}

@Preview(showBackground = true, name = "Длинное название, неактивна")
@Composable
private fun LongTextPreview() {
    SubTrackerTheme(dynamicColor = false) {
        EditSubscriptionScreen(
            draft = filledDraft.copy(
                name = "Очень длинное название подписки, которое не влезает в одну строку",
                amountText = "123456789.99",
                currency = "USD",
                isActive = false,
            ),
            onDraftChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "Альбомная", widthDp = 800, heightDp = 360)
@Composable
private fun LandscapePreview() {
    SubTrackerTheme(dynamicColor = false) {
        EditSubscriptionScreen(draft = filledDraft, onDraftChange = {}, onSave = {})
    }
}

@Preview(showBackground = true, name = "Узкий экран", widthDp = 280)
@Composable
private fun NarrowPreview() {
    SubTrackerTheme(dynamicColor = false) {
        EditSubscriptionScreen(draft = filledDraft, onDraftChange = {}, onSave = {})
    }
}
