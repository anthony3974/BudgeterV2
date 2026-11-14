package com.tony3974.budgeterv2.data

import android.content.Context
import android.util.AtomicFile
import com.tony3974.budgeterv2.budget.Income
import com.tony3974.budgeterv2.budget.Line
import com.tony3974.budgeterv2.budget.LineType
import com.tony3974.budgeterv2.budget.Money
import com.tony3974.budgeterv2.budget.Plan
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** An income as typed: [amount] is the raw text, so half-typed numbers survive. */
data class IncomeForm(val id: String, val name: String, val amount: String)

/** A budget line as typed. [value] is dollars for an amount, a percent for a percent. */
data class LineForm(
    val id: String,
    val name: String,
    val type: LineType,
    val value: String,
    /** The income that pays it, or null for all of them. */
    val sourceId: String?,
    val enabled: Boolean,
)

/** Everything on the screen, as typed. Saved as is; [toPlan] turns it into numbers. */
data class BudgetForm(val moneyNow: String, val incomes: List<IncomeForm>, val lines: List<LineForm>) {

    fun toPlan(): Plan = Plan(
        moneyNowCents = Money.parseCents(moneyNow) ?: 0,
        incomes = incomes.mapIndexed { i, it -> Income(it.id, incomeName(i, it), Money.parseCents(it.amount) ?: 0) },
        lines = lines.map {
            Line(
                id = it.id,
                name = it.name,
                type = it.type,
                amountCents = if (it.type == LineType.AMOUNT) Money.parseCents(it.value) ?: 0 else 0,
                percent = if (it.type == LineType.PERCENT) Money.parsePercent(it.value) ?: 0.0 else 0.0,
                sourceId = it.sourceId,
                enabled = it.enabled,
            )
        },
    )

    /** The name shown for an income: what was typed, or "Paycheck N". */
    fun incomeName(id: String?): String? = incomes.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        ?.let { incomeName(it, incomes[it]) }

    /** What "every income" is called: "both paychecks" with two. */
    val allIncomesName: String get() = if (incomes.size == 2) "Both paychecks" else "All income"

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        private fun incomeName(index: Int, income: IncomeForm) = income.name.ifBlank { "Paycheck ${index + 1}" }

        /** A first-run example: two paychecks, and the original Budgetor's lines spread over them. */
        fun example(): BudgetForm {
            val p1 = IncomeForm(newId(), "Paycheck 1", "")
            val p2 = IncomeForm(newId(), "Paycheck 2", "")
            fun line(name: String, type: LineType, value: String, source: String?) =
                LineForm(newId(), name, type, value, source, enabled = true)
            return BudgetForm(
                moneyNow = "",
                incomes = listOf(p1, p2),
                lines = listOf(
                    line("Insurance", LineType.AMOUNT, "125", p1.id),
                    line("YouTube", LineType.AMOUNT, "12", p2.id),
                    line("Gas", LineType.AMOUNT, "30", null),
                    line("Rec", LineType.AMOUNT, "48", null),
                    line("Other", LineType.PERCENT, "6", null),
                    line("Left over", LineType.LEFTOVER, "", null),
                ),
            )
        }
    }
}

/** Saves the form as JSON in the app's private files. */
object FormStore {
    private const val FILE = "budget.json"
    private const val VERSION = 1

    fun load(context: Context): BudgetForm {
        val file = AtomicFile(File(context.filesDir, FILE))
        if (!file.baseFile.exists()) return BudgetForm.example()
        return try {
            fromJson(JSONObject(String(file.readFully())))
        } catch (_: Exception) {
            BudgetForm.example()
        }
    }

    fun save(context: Context, form: BudgetForm) {
        val file = AtomicFile(File(context.filesDir, FILE))
        val out = file.startWrite()
        try {
            out.write(toJson(form).toString(2).toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    fun toJson(form: BudgetForm): JSONObject = JSONObject()
        .put("version", VERSION)
        .put("moneyNow", form.moneyNow)
        .put("incomes", JSONArray().apply {
            form.incomes.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount)) }
        })
        .put("lines", JSONArray().apply {
            form.lines.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("type", it.type.name)
                        .put("value", it.value)
                        .put("source", it.sourceId ?: JSONObject.NULL)
                        .put("enabled", it.enabled),
                )
            }
        })

    fun fromJson(json: JSONObject): BudgetForm {
        val incomes = json.getJSONArray("incomes").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                IncomeForm(o.getString("id"), o.optString("name"), o.optString("amount"))
            }
        }
        val lines = json.getJSONArray("lines").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                LineForm(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    type = LineType.entries.firstOrNull { it.name == o.optString("type") } ?: LineType.AMOUNT,
                    value = o.optString("value"),
                    sourceId = if (o.isNull("source")) null else o.optString("source"),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }
        return BudgetForm(json.optString("moneyNow"), incomes, lines)
    }
}
