package com.tony3974.budgeterv2.budget

import kotlin.math.roundToLong

/** How a budget line takes its money (the original Budgetor's Number, Percent and Remaining). */
enum class LineType {
    /** A fixed dollar amount; the result shows what share of the paycheck it is. */
    AMOUNT,

    /** A percent of the paycheck; the result shows it in dollars. */
    PERCENT,

    /** Takes nothing; shows what's left of the paycheck at that point in the list. */
    LEFTOVER,
}

/** One budget line. [amountCents] is used by amounts, [percent] by percents. */
data class Line(
    val id: String,
    val name: String,
    val type: LineType,
    val amountCents: Long = 0,
    val percent: Double = 0.0,
    val enabled: Boolean = true,
)

/** [paycheckCents] is both paychecks added together. Money is in cents. */
data class Plan(val moneyNowCents: Long, val paycheckCents: Long, val lines: List<Line>)

data class LineResult(
    /** What the line takes (0 for leftover lines and lines switched off). */
    val takenCents: Long,
    /** Amounts and percents: their share of the paycheck. Leftovers: the share left. */
    val share: Double?,
    /** Leftover lines only: what's left of the paycheck at this point in the list. */
    val leftoverCents: Long?,
)

data class BudgetResult(
    val lines: Map<String, LineResult>,
    val moneyNowCents: Long,
    val paycheckCents: Long,
    val budgetedCents: Long,
) {
    /** What's left of the paycheck after every line. */
    val leftCents: Long get() = paycheckCents - budgetedCents
    /** Money now, plus the paycheck, minus everything budgeted. */
    val endingCents: Long get() = moneyNowCents + leftCents
}

/** Works through the lines in order against one paycheck, as the original Budgetor did. */
object BudgetEngine {

    fun calculate(plan: Plan): BudgetResult {
        val pay = plan.paycheckCents
        var remaining = pay
        val results = LinkedHashMap<String, LineResult>()
        for (line in plan.lines) {
            if (!line.enabled) {
                results[line.id] = LineResult(0, null, null)
                continue
            }
            val taken = when (line.type) {
                LineType.AMOUNT -> line.amountCents
                LineType.PERCENT -> (pay * line.percent / 100.0).roundToLong()
                LineType.LEFTOVER -> {
                    results[line.id] = LineResult(0, shareOf(remaining, pay), remaining)
                    continue
                }
            }
            remaining -= taken
            results[line.id] = LineResult(taken, shareOf(taken, pay), null)
        }
        return BudgetResult(results, plan.moneyNowCents, pay, pay - remaining)
    }

    private fun shareOf(cents: Long, payCents: Long): Double? =
        if (payCents == 0L) null else cents.toDouble() / payCents
}
