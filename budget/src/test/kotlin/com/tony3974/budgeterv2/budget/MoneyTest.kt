package com.tony3974.budgeterv2.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun `parses what people type`() {
        assertEquals(123456L, Money.parseCents("$1,234.56"))
        assertEquals(123450L, Money.parseCents("1234.5"))
        assertEquals(-2000L, Money.parseCents("-20"))
        assertEquals(50L, Money.parseCents(".5"))
        assertEquals(100L, Money.parseCents(" 1 "))
        assertEquals(1L, Money.parseCents("0.005"))
    }

    @Test fun `empty or junk is null`() {
        assertNull(Money.parseCents(""))
        assertNull(Money.parseCents("-"))
        assertNull(Money.parseCents("."))
        assertNull(Money.parseCents("abc"))
        assertNull(Money.parseCents("1.2.3"))
    }

    @Test fun `percents`() {
        assertEquals(6.5, Money.parsePercent("6.5%")!!, 1e-9)
        assertEquals(6.5, Money.parsePercent("6,5")!!, 1e-9)
        assertNull(Money.parsePercent(""))
        assertNull(Money.parsePercent("x"))
    }
}
