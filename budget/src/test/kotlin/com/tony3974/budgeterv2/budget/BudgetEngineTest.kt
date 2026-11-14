package com.tony3974.budgeterv2.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetEngineTest {
    private val p1 = Income("p1", "Paycheck 1", 1_500_00)
    private val p2 = Income("p2", "Paycheck 2", 1_000_00)

    private fun amount(id: String, cents: Long, source: String? = null, enabled: Boolean = true) =
        Line(id, id, LineType.AMOUNT, amountCents = cents, sourceId = source, enabled = enabled)

    private fun percent(id: String, pct: Double, source: String? = null) =
        Line(id, id, LineType.PERCENT, percent = pct, sourceId = source)

    private fun leftover(id: String, source: String? = null) = Line(id, id, LineType.LEFTOVER, sourceId = source)

    private fun calc(vararg lines: Line, moneyNow: Long = 0, incomes: List<Income> = listOf(p1, p2)) =
        BudgetEngine.calculate(Plan(moneyNow, incomes, lines.toList()))

    @Test fun `an amount comes out of the paycheck it's assigned to`() {
        val r = calc(amount("rent", 600_00, "p1"))
        assertEquals(mapOf("p1" to 600_00L), r.lines.getValue("rent").perSource)
        assertEquals(0.4, r.lines.getValue("rent").share!!, 1e-9)
        assertEquals(900_00, r.sources[0].leftCents)
        assertEquals(1_000_00, r.sources[1].leftCents)
    }

    @Test fun `an amount from both paychecks is split evenly, odd cents first`() {
        val r = calc(amount("phone", 100_01))
        assertEquals(mapOf("p1" to 50_01L, "p2" to 50_00L), r.lines.getValue("phone").perSource)
        // Its share is of both paychecks together.
        assertEquals(100_01.0 / 2_500_00, r.lines.getValue("phone").share!!, 1e-9)
    }

    @Test fun `a percent comes out of each paycheck it draws on`() {
        val r = calc(percent("save", 10.0), percent("tithe", 5.0, "p2"))
        assertEquals(mapOf("p1" to 150_00L, "p2" to 100_00L), r.lines.getValue("save").perSource)
        assertEquals(mapOf("p2" to 50_00L), r.lines.getValue("tithe").perSource)
        assertEquals(250_00, r.lines.getValue("save").takenCents)
    }

    @Test fun `percents round to the cent`() {
        val r = calc(percent("x", 6.0), incomes = listOf(Income("a", "A", 333_33)))
        assertEquals(20_00, r.lines.getValue("x").takenCents) // 6% of 333.33 = 19.9998
    }

    @Test fun `a leftover line shows what's left at that point and takes nothing`() {
        val r = calc(amount("rent", 600_00, "p1"), leftover("mid", "p1"), amount("food", 200_00, "p1"), leftover("end", "p1"))
        assertEquals(900_00L, r.lines.getValue("mid").leftoverCents)
        assertEquals(0.6, r.lines.getValue("mid").share!!, 1e-9)
        assertEquals(700_00L, r.lines.getValue("end").leftoverCents)
        assertEquals(0, r.lines.getValue("end").takenCents)
        assertEquals(800_00, r.sources[0].budgetedCents)
    }

    @Test fun `a leftover over both paychecks adds them up`() {
        val r = calc(amount("rent", 600_00, "p1"), amount("car", 300_00, "p2"), leftover("left"))
        assertEquals(1_600_00L, r.lines.getValue("left").leftoverCents)
    }

    @Test fun `lines switched off count for nothing`() {
        val r = calc(amount("gym", 50_00, "p1", enabled = false))
        assertEquals(0, r.lines.getValue("gym").takenCents)
        assertNull(r.lines.getValue("gym").share)
        assertEquals(0, r.totalBudgetedCents)
    }

    @Test fun `a line whose paycheck was deleted draws on all of them`() {
        val r = calc(amount("rent", 200_00, "gone"))
        assertEquals(mapOf("p1" to 100_00L, "p2" to 100_00L), r.lines.getValue("rent").perSource)
    }

    @Test fun `ending is money now plus income minus everything budgeted`() {
        val r = calc(amount("rent", 600_00, "p1"), percent("save", 10.0), leftover("left"), moneyNow = 120_50)
        assertEquals(2_500_00, r.totalIncomeCents)
        assertEquals(850_00, r.totalBudgetedCents)
        assertEquals(1_650_00, r.totalLeftCents)
        assertEquals(1_770_50, r.endingCents)
    }

    @Test fun `going over a paycheck shows a negative left`() {
        val r = calc(amount("rent", 1_200_00, "p2"))
        assertEquals(-200_00, r.sources[1].leftCents)
    }

    @Test fun `a paycheck of zero gives no share instead of dividing by zero`() {
        val r = calc(amount("x", 10_00, "z"), leftover("l", "z"), incomes = listOf(Income("z", "Z", 0)))
        assertNull(r.lines.getValue("x").share)
        assertNull(r.lines.getValue("l").share)
    }

    @Test fun `no incomes still works`() {
        val r = calc(amount("x", 10_00), moneyNow = 5_00, incomes = emptyList())
        assertEquals(0, r.lines.getValue("x").takenCents)
        assertEquals(5_00, r.endingCents)
    }

    @Test fun `the original Budgetor's example still adds up for one paycheck`() {
        // Insurance 125, Other 6%, YouTube 12, Gas 15, Rec 24 out of a 1,000 paycheck.
        val pay = Income("pay", "Paycheck", 1_000_00)
        val r = calc(
            amount("Insurance", 125_00), percent("Other", 6.0), amount("YouTube", 12_00),
            amount("Gas", 15_00), amount("Rec", 24_00), leftover("Remaining"),
            moneyNow = 200_00, incomes = listOf(pay),
        )
        assertEquals(764_00L, r.lines.getValue("Remaining").leftoverCents)
        assertEquals(964_00, r.endingCents)
    }

    @Test fun `split evenly handles negatives and one part`() {
        assertEquals(listOf(-50L, -51L), BudgetEngine.splitEvenly(-101, 2).sortedDescending())
        assertEquals(listOf(7L), BudgetEngine.splitEvenly(7, 1))
        assertEquals(emptyList<Long>(), BudgetEngine.splitEvenly(7, 0))
    }
}
