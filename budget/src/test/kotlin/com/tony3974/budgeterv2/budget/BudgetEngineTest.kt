package com.tony3974.budgeterv2.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetEngineTest {
    // Both paychecks added together: 1,500 + 1,000.
    private val pay = 2_500_00L

    private fun amount(id: String, cents: Long, enabled: Boolean = true) =
        Line(id, id, LineType.AMOUNT, amountCents = cents, enabled = enabled)

    private fun percent(id: String, pct: Double) = Line(id, id, LineType.PERCENT, percent = pct)

    private fun leftover(id: String) = Line(id, id, LineType.LEFTOVER)

    private fun calc(vararg lines: Line, moneyNow: Long = 0, paycheck: Long = pay) =
        BudgetEngine.calculate(Plan(moneyNow, paycheck, lines.toList()))

    @Test fun `an amount shows its share of the combined paycheck`() {
        val r = calc(amount("rent", 1_000_00))
        assertEquals(1_000_00, r.lines.getValue("rent").takenCents)
        assertEquals(0.4, r.lines.getValue("rent").share!!, 1e-9)
        assertEquals(1_500_00, r.leftCents)
    }

    @Test fun `a percent is taken from the combined paycheck`() {
        val r = calc(percent("save", 10.0))
        assertEquals(250_00, r.lines.getValue("save").takenCents)
        assertEquals(0.1, r.lines.getValue("save").share!!, 1e-9)
    }

    @Test fun `percents round to the cent`() {
        val r = calc(percent("x", 6.0), paycheck = 333_33)
        assertEquals(20_00, r.lines.getValue("x").takenCents) // 6% of 333.33 = 19.9998
    }

    @Test fun `a leftover line shows what's left at that point and takes nothing`() {
        val r = calc(amount("rent", 1_000_00), leftover("mid"), amount("food", 500_00), leftover("end"))
        assertEquals(1_500_00L, r.lines.getValue("mid").leftoverCents)
        assertEquals(0.6, r.lines.getValue("mid").share!!, 1e-9)
        assertEquals(1_000_00L, r.lines.getValue("end").leftoverCents)
        assertEquals(0, r.lines.getValue("end").takenCents)
        assertEquals(1_500_00, r.budgetedCents)
    }

    @Test fun `lines switched off count for nothing`() {
        val r = calc(amount("gym", 50_00, enabled = false))
        assertEquals(0, r.lines.getValue("gym").takenCents)
        assertNull(r.lines.getValue("gym").share)
        assertEquals(0, r.budgetedCents)
    }

    @Test fun `ending is money now plus the paycheck minus everything budgeted`() {
        val r = calc(amount("rent", 600_00), percent("save", 10.0), leftover("left"), moneyNow = 120_50)
        assertEquals(850_00, r.budgetedCents)
        assertEquals(1_650_00, r.leftCents)
        assertEquals(1_770_50, r.endingCents)
    }

    @Test fun `going over the paycheck shows negative left`() {
        val r = calc(amount("rent", 3_000_00), leftover("left"))
        assertEquals(-500_00L, r.lines.getValue("left").leftoverCents)
        assertEquals(-500_00, r.leftCents)
    }

    @Test fun `a paycheck of zero gives no share instead of dividing by zero`() {
        val r = calc(amount("x", 10_00), leftover("l"), moneyNow = 5_00, paycheck = 0)
        assertNull(r.lines.getValue("x").share)
        assertNull(r.lines.getValue("l").share)
        assertEquals(-5_00, r.endingCents)
    }

    @Test fun `the original Budgetor's example still adds up`() {
        // Insurance 125, Other 6%, YouTube 12, Gas 15, Rec 24 out of a 1,000 paycheck.
        val r = calc(
            amount("Insurance", 125_00), percent("Other", 6.0), amount("YouTube", 12_00),
            amount("Gas", 15_00), amount("Rec", 24_00), leftover("Remaining"),
            moneyNow = 200_00, paycheck = 1_000_00,
        )
        assertEquals(764_00L, r.lines.getValue("Remaining").leftoverCents)
        assertEquals(964_00, r.endingCents)
    }
}
