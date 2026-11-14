package com.tony3974.budgeterv2.budget

import kotlin.math.roundToLong

/** How a budget line takes its money. */
enum class LineType {
    /** A fixed dollar amount; the result shows what share of the paycheck it is. */
    AMOUNT,

    /** A percent of the paycheck; the result shows it in dollars. */
    PERCENT,

    /** Takes nothing; shows what's left of the paycheck at that point in the list. */
    LEFTOVER,
}

/** A source of income for this period, e.g. one paycheck. Money is in cents. */
data class Income(val id: String, val name: String, val cents: Long)

/**
 * One budget line. [sourceId] is the income that pays it, or null for all of them ("both
 * paychecks"): an amount is then split evenly between them, a percent comes out of each.
 */
data class Line(
    val id: String,
    val name: String,
    val type: LineType,
    val amountCents: Long = 0,
    val percent: Double = 0.0,
    val sourceId: String? = null,
    val enabled: Boolean = true,
)

data class Plan(val moneyNowCents: Long, val incomes: List<Income>, val lines: List<Line>)

data class LineResult(
    /** What the line takes in total (0 for leftover lines and lines switched off). */
    val takenCents: Long,
    /** What it takes from each income, by income id. */
    val perSource: Map<String, Long>,
    /** For amounts and percents: the share of the income(s) it draws on. For leftovers: the share left. */
    val share: Double?,
    /** Leftover lines only: what's left of the income(s) at this point in the list. */
    val leftoverCents: Long?,
    /** The incomes this line draws on. */
    val sourceIds: List<String>,
)

data class SourceResult(val income: Income, val budgetedCents: Long) {
    val leftCents: Long get() = income.cents - budgetedCents
}

data class BudgetResult(
    val lines: Map<String, LineResult>,
    val sources: List<SourceResult>,
    val moneyNowCents: Long,
) {
    val totalIncomeCents: Long get() = sources.sumOf { it.income.cents }
    val totalBudgetedCents: Long get() = sources.sumOf { it.budgetedCents }
    val totalLeftCents: Long get() = totalIncomeCents - totalBudgetedCents
    /** Money now, plus all income, minus everything budgeted: what you'll have after these paychecks. */
    val endingCents: Long get() = moneyNowCents + totalLeftCents
}

/**
 * Works through the budget lines in order, the way the original Budgetor did for a single
 * paycheck, but with any number of incomes. Each line draws on one income or on all of them.
 */
object BudgetEngine {

    fun calculate(plan: Plan): BudgetResult {
        val remaining = LinkedHashMap<String, Long>()
        val budgeted = LinkedHashMap<String, Long>()
        plan.incomes.forEach {
            remaining[it.id] = it.cents
            budgeted[it.id] = 0
        }
        val results = LinkedHashMap<String, LineResult>()

        for (line in plan.lines) {
            // An income that was deleted falls back to all incomes.
            val sources = plan.incomes.filter { it.id == line.sourceId }.ifEmpty { plan.incomes }
            val ids = sources.map { it.id }
            val base = sources.sumOf { it.cents }
            if (!line.enabled || sources.isEmpty()) {
                results[line.id] = LineResult(0, emptyMap(), null, null, ids)
                continue
            }

            val perSource: Map<String, Long> = when (line.type) {
                LineType.AMOUNT -> ids.zip(splitEvenly(line.amountCents, ids.size)).toMap()
                LineType.PERCENT -> sources.associate { it.id to (it.cents * line.percent / 100.0).roundToLong() }
                LineType.LEFTOVER -> {
                    val left = ids.sumOf { remaining.getValue(it) }
                    results[line.id] = LineResult(0, emptyMap(), shareOf(left, base), left, ids)
                    continue
                }
            }
            perSource.forEach { (id, cents) ->
                remaining[id] = remaining.getValue(id) - cents
                budgeted[id] = budgeted.getValue(id) + cents
            }
            val taken = perSource.values.sum()
            results[line.id] = LineResult(taken, perSource, shareOf(taken, base), null, ids)
        }

        val sources = plan.incomes.map { SourceResult(it, budgeted.getValue(it.id)) }
        return BudgetResult(results, sources, plan.moneyNowCents)
    }

    /** [cents] in [parts] near-equal pieces; the odd cents go to the first pieces. */
    fun splitEvenly(cents: Long, parts: Int): List<Long> {
        if (parts <= 0) return emptyList()
        val each = Math.floorDiv(cents, parts.toLong())
        val extra = Math.floorMod(cents, parts.toLong()).toInt()
        return List(parts) { i -> each + if (i < extra) 1 else 0 }
    }

    private fun shareOf(cents: Long, baseCents: Long): Double? =
        if (baseCents == 0L) null else cents.toDouble() / baseCents
}
