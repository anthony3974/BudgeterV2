package com.tony3974.budgeterv2.budget

import java.math.BigDecimal
import java.math.RoundingMode

/** Reading what's typed into the money and percent fields. */
object Money {
    /** "$1,234.56", "1234.5", "-20" → cents. Null when empty or not a number. Rounds to the cent. */
    fun parseCents(text: String): Long? {
        val t = text.trim().replace("$", "").replace(",", "").replace(" ", "")
        if (t.isEmpty() || t == "-" || t == ".") return null
        return try {
            BigDecimal(t).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** "6", "6.5%", "6,5" → 6.5. Null when empty or not a number. */
    fun parsePercent(text: String): Double? =
        text.trim().removeSuffix("%").trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}
