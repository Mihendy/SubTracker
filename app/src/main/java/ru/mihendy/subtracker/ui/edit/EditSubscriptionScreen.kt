package ru.mihendy.subtracker.ui.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
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
import ru.mihendy.subtracker.ui.common.SubscriptionCard
import ru.mihendy.subtracker.ui.theme.SubTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

/*
 * Экран создания/редактирования подписки.
 *
 * Все функции здесь stateless: получают значения параметрами и сообщают
 * об изменениях колбэками («состояние вниз, события вверх»). Где хранится
 * черновик — решает вызывающий код (см. EditSubscriptionRoute). Поэтому
 * любую функцию можно показать в @Preview без заглушек.
 */

/** Цвета-метки на выбор (ARGB): белый и приглушённые цвета радуги. «Без метки» — отдельный квадратик. */
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
    // imePadding уменьшает область на высоту клавиатуры, а поле в фокусе
    // само прокручивается в видимую часть — так клавиатура его не закрывает.
    // На широком экране форма не растягивается больше FORM_MAX_WIDTH и стоит по центру.
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
        SubscriptionCard(
            name = draft.name,
            amount = draft.amount?.toPlainString(),
            currency = draft.currency,
            nextPaymentDate = draft.nextPaymentDate,
            color = draft.color,
            isActive = draft.isActive,
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
            onNextPaymentDateChange = { onDraftChange(draft.copy(nextPaymentDate = it)) },
            onEndDateChange = { onDraftChange(draft.copy(endDate = it)) },
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
        NoteField(
            note = draft.noteText,
            onNoteChange = { onDraftChange(draft.copy(noteText = it)) },
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
 * Даты списания и окончания. Нажатие на дату открывает стандартный календарь.
 * Дату окончания можно сбросить кнопкой «Бессрочно».
 */
@Composable
fun DatesSection(
    nextPaymentDate: LocalDate,
    endDate: LocalDate?,
    onNextPaymentDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DateField(
            label = stringResource(R.string.edit_next_payment),
            date = nextPaymentDate,
            onDateSelected = onNextPaymentDateChange,
        )
        DateField(
            label = stringResource(R.string.edit_end_date),
            date = endDate,
            onDateSelected = onEndDateChange,
            onClear = { onEndDateChange(null) },
        )
    }
}

/**
 * Строка «подпись — дата». Дата — кнопка, по нажатию открывается календарь.
 *
 * Открыт ли календарь — внутреннее состояние этой строки (оно никому снаружи
 * не нужно), поэтому хранится здесь. rememberSaveable — чтобы открытый
 * календарь не закрылся при повороте экрана.
 *
 * @param date выбранная дата; `null` — не задана (показывается «бессрочно»).
 * @param onClear если задан, в календаре появляется кнопка «Бессрочно».
 */
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { showPicker = true }) {
            Text(date?.format(formatter) ?: stringResource(R.string.edit_end_date_none))
        }
    }

    if (showPicker) {
        DatePickerModal(
            initialDate = date,
            onConfirm = onDateSelected,
            onClear = onClear,
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Стандартный диалог выбора даты из Material 3.
 *
 * DatePicker работает с миллисекундами в UTC, поэтому дата переводится
 * туда и обратно через [toUtcMillis] / [utcMillisToLocalDate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate?.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onConfirm(utcMillisToLocalDate(it)) }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) {
                        Text(stringResource(R.string.edit_end_date_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Начало дня [this] в UTC, в миллисекундах — формат, который понимает DatePicker. */
private fun LocalDate.toUtcMillis(): Long = TimeUnit.DAYS.toMillis(toEpochDay())

/** Обратно: миллисекунды UTC от DatePicker → дата. */
private fun utcMillisToLocalDate(millis: Long): LocalDate =
    LocalDate.ofEpochDay(Math.floorDiv(millis, TimeUnit.DAYS.toMillis(1)))

/**
 * Заметка. Поле растёт по мере ввода текста, но не бывает ниже трёх строк
 * ([minLines]); верхнего предела нет — длинную заметку прокручивает вся форма.
 */
@Composable
fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text(stringResource(R.string.edit_note)) },
        minLines = 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Выбор цветовой метки: квадратики с чёрной рамкой, на выбранном — галочка.
 * Первый квадратик — перечёркнутый «без метки» (`null`), за ним цвета из [colors].
 * [FlowRow] переносит квадратики на новую строку, если они не помещаются по ширине.
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
            ColorSwatch(
                color = null,
                isSelected = selected == null,
                description = stringResource(R.string.edit_color_none),
                onClick = { onSelect(null) },
            )
            colors.forEachIndexed { index, color ->
                ColorSwatch(
                    color = Color(color),
                    isSelected = color == selected,
                    description = stringResource(R.string.edit_color_item, index + 1),
                    onClick = { onSelect(color) },
                )
            }
        }
    }
}

private val SwatchShape = RoundedCornerShape(4.dp)

/**
 * Один квадратик выбора цвета: чёрная рамка, на выбранном — галочка.
 * [color] == `null` — «без метки»: пустой квадратик с диагональю.
 *
 * Галочка чёрная на светлых цветах и белая на тёмных — чтобы её было видно на любом.
 */
@Composable
private fun ColorSwatch(
    color: Color?,
    isSelected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkColor = if (color == null || color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(SwatchShape)
            .then(
                if (color != null) {
                    Modifier.background(color)
                } else {
                    // Диагональ из левого нижнего угла в правый верхний
                    Modifier.drawBehind {
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            )
            .border(1.dp, Color.Black, SwatchShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                // Описание уже есть у квадратика (semantics выше), галочке своё не нужно
                contentDescription = null,
                tint = checkColor,
            )
        }
    }
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
    noteText = "Оплата с карты *1234",
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
                noteText = "Длинная заметка.\nВторая строка.\nТретья строка.\nЧетвёртая — поле выросло.",
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
