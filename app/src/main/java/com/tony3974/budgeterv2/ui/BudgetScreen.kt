package com.tony3974.budgeterv2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tony3974.budgeterv2.BudgetViewModel
import com.tony3974.budgeterv2.R
import com.tony3974.budgeterv2.budget.BudgetResult
import com.tony3974.budgeterv2.budget.LineResult
import com.tony3974.budgeterv2.budget.LineType
import com.tony3974.budgeterv2.data.BudgetForm
import com.tony3974.budgeterv2.data.IncomeForm
import com.tony3974.budgeterv2.data.LineForm
import java.math.BigDecimal
import java.text.NumberFormat

private fun money(cents: Long): String = NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(cents, 2))

private fun percent(share: Double): String = "%.1f%%".format(share * 100)

private val LineType.label: String
    get() = when (this) {
        LineType.AMOUNT -> "$ Amount"
        LineType.PERCENT -> "% Percent"
        LineType.LEFTOVER -> "Left over"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: BudgetViewModel = viewModel()) {
    val form by vm.form.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budgeter V2") }) },
        bottomBar = { EndingBar(result) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MoneyField(
                value = form.moneyNow,
                onValueChange = vm::setMoneyNow,
                label = "Money now",
                supporting = "What's in your account before these paychecks",
                modifier = Modifier.fillMaxWidth(),
            )
            IncomeSection(form, vm)
            LinesSection(form, result, vm)
            SummarySection(form, result)
        }
    }
}

@Composable
private fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("$") },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.keepVisibleWhenFocused(),
    )
}

// ---- Income ----

@Composable
private fun IncomeSection(form: BudgetForm, vm: BudgetViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Income") {
            TextButton(onClick = vm::addIncome) { Text("+ Add income") }
        }
        form.incomes.forEachIndexed { i, income ->
            IncomeRow(i, income, canRemove = form.incomes.size > 1, vm)
        }
    }
}

@Composable
private fun IncomeRow(index: Int, income: IncomeForm, canRemove: Boolean, vm: BudgetViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = income.name,
            onValueChange = { vm.updateIncome(income.copy(name = it)) },
            label = { Text("Name") },
            placeholder = { Text("Paycheck ${index + 1}") },
            singleLine = true,
            modifier = Modifier.weight(1f).keepVisibleWhenFocused(),
        )
        MoneyField(
            value = income.amount,
            onValueChange = { vm.updateIncome(income.copy(amount = it)) },
            label = "Amount",
            modifier = Modifier.weight(1f),
        )
        if (canRemove) {
            IconButton(onClick = { vm.removeIncome(income.id) }) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Remove income")
            }
        }
    }
}

// ---- Budget lines ----

@Composable
private fun LinesSection(form: BudgetForm, result: BudgetResult, vm: BudgetViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Budget") {
            TextButton(onClick = vm::addLine) { Text("+ Add line") }
        }
        if (form.lines.isEmpty()) Text("No lines yet. Add rent, bills, savings…")
        form.lines.forEachIndexed { i, line ->
            LineCard(
                line = line,
                result = result.lines[line.id],
                form = form,
                isFirst = i == 0,
                isLast = i == form.lines.lastIndex,
                vm = vm,
            )
        }
        if (form.lines.isNotEmpty()) {
            FilledTonalButton(onClick = vm::addLine) { Text("+ Add line") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineCard(
    line: LineForm,
    result: LineResult?,
    form: BudgetForm,
    isFirst: Boolean,
    isLast: Boolean,
    vm: BudgetViewModel,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = line.enabled, onCheckedChange = { vm.updateLine(line.copy(enabled = it)) })
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = line.name,
                    onValueChange = { vm.updateLine(line.copy(name = it)) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).keepVisibleWhenFocused(),
                )
                LineMenu(isFirst, isLast, onMove = { vm.moveLine(line.id, it) }, onDelete = { vm.removeLine(line.id) })
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                LineType.entries.forEachIndexed { i, type ->
                    SegmentedButton(
                        selected = line.type == type,
                        onClick = { vm.updateLine(line.copy(type = type)) },
                        shape = SegmentedButtonDefaults.itemShape(i, LineType.entries.size),
                        label = { Text(type.label, maxLines = 1) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (line.type) {
                    LineType.AMOUNT -> MoneyField(
                        value = line.value,
                        onValueChange = { vm.updateLine(line.copy(value = it)) },
                        label = "Amount",
                        modifier = Modifier.weight(1f),
                    )
                    LineType.PERCENT -> OutlinedTextField(
                        value = line.value,
                        onValueChange = { vm.updateLine(line.copy(value = it)) },
                        label = { Text("Percent") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).keepVisibleWhenFocused(),
                    )
                    LineType.LEFTOVER -> Text(
                        "Shows what's left at this point in the list.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
                SourcePicker(line.sourceId, form) { vm.updateLine(line.copy(sourceId = it)) }
            }
            val (text, over) = resultText(line, result, form)
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LineMenu(isFirst: Boolean, isLast: Boolean, onMove: (Int) -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Move up") }, enabled = !isFirst, onClick = { open = false; onMove(-1) })
            DropdownMenuItem(text = { Text("Move down") }, enabled = !isLast, onClick = { open = false; onMove(1) })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/** "From: Paycheck 1 ▾", choosing which income pays a line. */
@Composable
private fun SourcePicker(sourceId: String?, form: BudgetForm, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = form.incomeName(sourceId) ?: form.allIncomesName
    Box {
        OutlinedButton(onClick = { open = true }) { Text("From: $label ▾", maxLines = 1) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            form.incomes.forEach { income ->
                DropdownMenuItem(
                    text = { Text(form.incomeName(income.id) ?: "") },
                    onClick = { open = false; onPick(income.id) },
                )
            }
            DropdownMenuItem(text = { Text(form.allIncomesName) }, onClick = { open = false; onPick(null) })
        }
    }
}

/** What a line works out to, and whether it's a problem (left over below zero). */
private fun resultText(line: LineForm, r: LineResult?, form: BudgetForm): Pair<String, Boolean> {
    if (r == null || !line.enabled) return "Off: not counted" to false
    val from = form.incomeName(line.sourceId) ?: form.allIncomesName.lowercase()
    val split = r.perSource.size > 1
    val parts = if (split) " (" + r.perSource.values.joinToString(" + ") { money(it) } + ")" else ""
    return when (line.type) {
        LineType.AMOUNT -> {
            val share = r.share?.let { "${percent(it)} of $from" } ?: "Enter the paycheck amount to see its share"
            share + parts to false
        }
        LineType.PERCENT -> "${money(r.takenCents)} from $from$parts" to false
        LineType.LEFTOVER -> {
            val left = r.leftoverCents ?: 0
            "${money(left)} left of $from" + (r.share?.let { " (${percent(it)})" } ?: "") to (left < 0)
        }
    }
}

// ---- Summary ----

@Composable
private fun SummarySection(form: BudgetForm, result: BudgetResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Summary")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                result.sources.forEach { s ->
                    Text(form.incomeName(s.income.id) ?: s.income.name, style = MaterialTheme.typography.titleMedium)
                    SummaryRow("Income", money(s.income.cents))
                    SummaryRow("Budgeted", "− " + money(s.budgetedCents))
                    SummaryRow("Left", money(s.leftCents), strong = true, bad = s.leftCents < 0)
                    HorizontalDivider()
                }
                SummaryRow("Money now", money(result.moneyNowCents))
                SummaryRow("All income", "+ " + money(result.totalIncomeCents))
                SummaryRow("Everything budgeted", "− " + money(result.totalBudgetedCents))
                SummaryRow("After these paychecks", money(result.endingCents), strong = true, bad = result.endingCents < 0)
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, strong: Boolean = false, bad: Boolean = false) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (strong) FontWeight.SemiBold else null,
            color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Always on screen: what you'll have after these paychecks. */
@Composable
private fun EndingBar(result: BudgetResult) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("After these paychecks", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Left from income: ${money(result.totalLeftCents)}",
                    style = MaterialTheme.typography.bodySmall,
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
