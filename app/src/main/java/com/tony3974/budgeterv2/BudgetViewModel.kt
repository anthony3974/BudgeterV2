package com.tony3974.budgeterv2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tony3974.budgeterv2.budget.BudgetEngine
import com.tony3974.budgeterv2.budget.BudgetResult
import com.tony3974.budgeterv2.data.BudgetForm
import com.tony3974.budgeterv2.data.FormStore
import com.tony3974.budgeterv2.data.IncomeForm
import com.tony3974.budgeterv2.data.LineForm
import com.tony3974.budgeterv2.budget.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds the form, works out the result as it changes, and saves it shortly after each edit. */
@OptIn(FlowPreview::class)
class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val _form = MutableStateFlow(FormStore.load(app))
    val form: StateFlow<BudgetForm> = _form

    val result: StateFlow<BudgetResult> = _form
        .map { BudgetEngine.calculate(it.toPlan()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BudgetEngine.calculate(_form.value.toPlan()))

    init {
        viewModelScope.launch {
            _form.drop(1).debounce(400).collect { save(it) }
        }
    }

    /** Saves now, e.g. when the app goes to the background. */
    fun flush() {
        viewModelScope.launch { save(_form.value) }
    }

    private suspend fun save(form: BudgetForm) = withContext(Dispatchers.IO) {
        FormStore.save(getApplication(), form)
    }

    private fun update(change: (BudgetForm) -> BudgetForm) {
        _form.value = change(_form.value)
    }

    fun setMoneyNow(text: String) = update { it.copy(moneyNow = text) }

    fun updateIncome(income: IncomeForm) = update { f ->
        f.copy(incomes = f.incomes.map { if (it.id == income.id) income else it })
    }

    fun addIncome() = update { it.copy(incomes = it.incomes + IncomeForm(BudgetForm.newId(), "", "")) }

    /** Lines that were paid by it move to all incomes. */
    fun removeIncome(id: String) = update { f ->
        f.copy(
            incomes = f.incomes.filterNot { it.id == id },
            lines = f.lines.map { if (it.sourceId == id) it.copy(sourceId = null) else it },
        )
    }

    fun updateLine(line: LineForm) = update { f ->
        f.copy(lines = f.lines.map { if (it.id == line.id) line else it })
    }

    fun addLine() = update {
        it.copy(lines = it.lines + LineForm(BudgetForm.newId(), "", LineType.AMOUNT, "", null, enabled = true))
    }

    fun removeLine(id: String) = update { f -> f.copy(lines = f.lines.filterNot { it.id == id }) }

    fun moveLine(id: String, by: Int) = update { f ->
        val from = f.lines.indexOfFirst { it.id == id }
        val to = (from + by).coerceIn(0, f.lines.lastIndex)
        if (from < 0 || from == to) f else f.copy(lines = f.lines.toMutableList().apply { add(to, removeAt(from)) })
    }
}
