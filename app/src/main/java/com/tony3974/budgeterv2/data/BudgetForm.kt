package com.tony3974.budgeterv2.data

import android.content.Context
import android.util.AtomicFile
import com.tony3974.budgeterv2.budget.Line
import com.tony3974.budgeterv2.budget.LineType
import com.tony3974.budgeterv2.budget.Money
import com.tony3974.budgeterv2.budget.Plan
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.util.UUID

/** A budget line as typed. [value] is dollars for an amount, a percent for a percent. */
data class LineForm(
    val id: String,
    val name: String,
    val type: LineType,
    val value: String,
    val enabled: Boolean,
)

/** Everything on the screen, as typed (so half-typed numbers survive). [toPlan] turns it into numbers. */
data class BudgetForm(
    val moneyNow: String,
    val paycheck1: String,
    val paycheck2: String,
    val lines: List<LineForm>,
) {
    /** Both paychecks added together. */
    val paycheckCents: Long get() = (Money.parseCents(paycheck1) ?: 0) + (Money.parseCents(paycheck2) ?: 0)

    fun toPlan(): Plan = Plan(
        moneyNowCents = Money.parseCents(moneyNow) ?: 0,
        paycheckCents = paycheckCents,
        lines = lines.map {
            Line(
                id = it.id,
                name = it.name,
                type = it.type,
                amountCents = if (it.type == LineType.AMOUNT) Money.parseCents(it.value) ?: 0 else 0,
                percent = if (it.type == LineType.PERCENT) Money.parsePercent(it.value) ?: 0.0 else 0.0,
                enabled = it.enabled,
            )
        },
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        fun newLine() = LineForm(newId(), "", LineType.AMOUNT, "", enabled = true)

        /** A first-run example: the original Budgetor's lines. */
        fun example(): BudgetForm {
            fun line(name: String, type: LineType, value: String) = LineForm(newId(), name, type, value, enabled = true)
            return BudgetForm(
                moneyNow = "",
                paycheck1 = "",
                paycheck2 = "",
                lines = listOf(
                    line("Insurance", LineType.AMOUNT, "125"),
                    line("Other", LineType.PERCENT, "6"),
                    line("YouTube", LineType.AMOUNT, "12"),
                    line("Gas", LineType.AMOUNT, "30"),
                    line("Rec", LineType.AMOUNT, "48"),
                    line("Left over", LineType.LEFTOVER, ""),
                ),
            )
        }
    }
}

/** Saves the form as JSON in the app's private files. */
object FormStore {
    private const val FILE = "budget.json"
    private const val VERSION = 2

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
        .put("paycheck1", form.paycheck1)
        .put("paycheck2", form.paycheck2)
        .put("lines", JSONArray().apply {
            form.lines.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("type", it.type.name)
                        .put("value", it.value)
                        .put("enabled", it.enabled),
                )
            }
        })

    fun fromJson(json: JSONObject): BudgetForm {
        val lines = json.getJSONArray("lines").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                LineForm(
                    id = o.optString("id").ifEmpty { BudgetForm.newId() },
                    name = o.optString("name"),
                    type = LineType.entries.firstOrNull { it.name == o.optString("type") } ?: LineType.AMOUNT,
                    value = o.optString("value"),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }
        // Version 1 kept a list of incomes; the first is Paycheck 1, the rest add up to Paycheck 2.
        val incomes = json.optJSONArray("incomes")
        val (pay1, pay2) = if (incomes != null) {
            val amounts = List(incomes.length()) { incomes.getJSONObject(it).optString("amount") }
            val rest = amounts.drop(1)
            val second = if (rest.size <= 1) rest.firstOrNull().orEmpty() else
                BigDecimal.valueOf(rest.sumOf { Money.parseCents(it) ?: 0 }, 2).toPlainString()
            amounts.firstOrNull().orEmpty() to second
        } else {
            json.optString("paycheck1") to json.optString("paycheck2")
        }
        return BudgetForm(json.optString("moneyNow"), pay1, pay2, lines)
    }
}
