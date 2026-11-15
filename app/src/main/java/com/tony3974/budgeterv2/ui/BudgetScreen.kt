package com.tony3974.budgeterv2.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tony3974.budgeterv2.BudgetViewModel
import com.tony3974.budgeterv2.budget.BudgetResult
import com.tony3974.budgeterv2.budget.LineResult
import com.tony3974.budgeterv2.budget.LineType
import com.tony3974.budgeterv2.data.LineForm
import java.math.BigDecimal
import java.text.NumberFormat

private val FieldHeight = 40.dp
private val ValueWidth = 76.dp
private val ResultWidth = 86.dp

/** Space from the screen edge, shared by the content and the totals bar so their edges line up. */
private val EdgePadding = 12.dp
/** Space between the boxes in a row. */
private val ItemGap = 6.dp
/** Space between rows. */
private val RowGap = 8.dp
/** The drawn size of a Material checkbox, without its padding or touch area. */
private val CheckboxBox = 20.dp

private fun money(cents: Long): String = NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(cents, 2))

private fun percent(share: Double): String = "%.1f%%".format(share * 100)

private val LineType.symbol: String
    get() = when (this) {
        LineType.AMOUNT -> "$"
        LineType.PERCENT -> "%"
        LineType.LEFTOVER -> "="
    }

private val LineType.label: String
    get() = when (this) {
        LineType.AMOUNT -> "Amount: shows its % of the paycheck"
        LineType.PERCENT -> "Percent: shows it in dollars"
        LineType.LEFTOVER -> "Left over: shows what's left here"
    }

/**
 * One compact screen, laid out like the original Budgetor: money now and the two paychecks on
 * top, then one row per line (on/off, type, name, value, result), and the totals in a bar at the
 * bottom. The two paychecks are added together into one paycheck that every line draws on.
 */
@Composable
fun BudgetScreen(vm: BudgetViewModel = viewModel()) {
    val form by vm.form.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }

    Scaffold(bottomBar = { TotalsBar(result) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                // Takes the initial focus (given out when there's a hardware keyboard), so the
                // first text box doesn't grab it and open the on-screen keyboard at launch.
                .focusable()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EdgePadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
                LabeledField("Money now", form.moneyNow, vm::setMoneyNow, Modifier.weight(1f))
                LabeledField("Paycheck 1", form.paycheck1, vm::setPaycheck1, Modifier.weight(1f))
                LabeledField("Paycheck 2", form.paycheck2, vm::setPaycheck2, Modifier.weight(1f))
            }
            Text(
                "Paycheck: ${money(result.paycheckCents)} (both added together)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            HorizontalDivider()
            form.lines.forEachIndexed { i, line ->
                LineRow(
                    line = line,
                    result = result.lines[line.id],
                    isFirst = i == 0,
                    isLast = i == form.lines.lastIndex,
                    vm = vm,
                )
            }
            if (form.lines.isEmpty()) {
                Text("No lines yet.", Modifier.padding(vertical = 8.dp))
            }
            // No side padding, so the text lines up with the left edge of everything above it.
            TextButton(onClick = vm::addLine, contentPadding = PaddingValues(vertical = 8.dp)) {
                Text("+ Add line")
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CompactField(value, onValueChange, placeholder = "0", numeric = true, prefix = "$", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LineRow(line: LineForm, result: LineResult?, isFirst: Boolean, isLast: Boolean, vm: BudgetViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides FieldHeight) {
            Checkbox(
                checked = line.enabled,
                onCheckedChange = { vm.updateLine(line.copy(enabled = it)) },
                modifier = Modifier.tightTouchArea(),
            )
        }
        TypeButton(
            type = line.type,
            isFirst = isFirst,
            isLast = isLast,
            onType = { vm.updateLine(line.copy(type = it)) },
            onMove = { vm.moveLine(line.id, it) },
            onDelete = { vm.removeLine(line.id) },
        )
        CompactField(
            value = line.name,
            onValueChange = { vm.updateLine(line.copy(name = it)) },
            placeholder = "Name",
            modifier = Modifier.weight(1f),
        )
        if (line.type == LineType.LEFTOVER) {
            Spacer(Modifier.width(ValueWidth))
        } else {
            CompactField(
                value = line.value,
                onValueChange = { vm.updateLine(line.copy(value = it)) },
                placeholder = "0",
                numeric = true,
                prefix = if (line.type == LineType.AMOUNT) "$" else null,
                suffix = if (line.type == LineType.PERCENT) "%" else null,
                modifier = Modifier.width(ValueWidth),
            )
        }
        val (text, bad) = resultText(line, result)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (line.type == LineType.LEFTOVER) FontWeight.SemiBold else null,
            color = when {
                bad -> MaterialTheme.colorScheme.error
                !line.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            },
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp).width(ResultWidth),
        )
    }
}

/**
 * Takes up only the width of the checkbox's drawn box (20 dp) in the row while keeping its full
 * 40 dp touch area, which spills 10 dp past each side. Without this the box sits 10 dp in from the
 * left edge and has a wide gap after it.
 */
private fun Modifier.tightTouchArea(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val inset = ((placeable.width - CheckboxBox.roundToPx()) / 2).coerceAtLeast(0)
    layout(placeable.width - 2 * inset, placeable.height) { placeable.place(-inset, 0) }
}

/** What a line works out to, as the original showed it, and whether it's a problem. */
private fun resultText(line: LineForm, r: LineResult?): Pair<String, Boolean> {
    if (r == null || !line.enabled) return "off" to false
    return when (line.type) {
        LineType.AMOUNT -> (r.share?.let { percent(it) } ?: "–") to false
        LineType.PERCENT -> money(r.takenCents) to false
        LineType.LEFTOVER -> {
            val left = r.leftoverCents ?: 0
            money(left) to (left < 0)
        }
    }
}

/** A small square showing the line's type ($, %, =). Tapping it opens the line's menu. */
@Composable
private fun TypeButton(
    type: LineType,
    isFirst: Boolean,
    isLast: Boolean,
    onType: (LineType) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(FieldHeight)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(type.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LineType.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${t.symbol}  ${t.label}",
                            fontWeight = if (t == type) FontWeight.Bold else null,
                        )
                    },
                    onClick = { open = false; onType(t) },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Move up") }, enabled = !isFirst, onClick = { open = false; onMove(-1) })
            DropdownMenuItem(text = { Text("Move down") }, enabled = !isLast, onClick = { open = false; onMove(1) })
            DropdownMenuItem(
                text = { Text("Delete line", color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/** A 40 dp text box (Material's text fields are 56 dp and much roomier than this screen wants). */
@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    prefix: String? = null,
    suffix: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        interactionSource = interaction,
        modifier = modifier.height(FieldHeight).keepVisibleWhenFocused(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color = if (focused) colors.primary else colors.outline,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prefix != null) Text(prefix, style = textStyle, color = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = textStyle, color = colors.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1)
                    }
                    inner()
                }
                if (suffix != null) Text(suffix, style = textStyle, color = colors.onSurfaceVariant)
            }
        },
    )
}

/** Always on screen: what's budgeted, what's left, and what you'll have after these paychecks. */
@Composable
private fun TotalsBar(result: BudgetResult) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = EdgePadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("After these paychecks", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Budgeted ${money(result.budgetedCents)} · left ${money(result.leftCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.leftCents < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
            Text(
                money(result.endingCents),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (result.endingCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
