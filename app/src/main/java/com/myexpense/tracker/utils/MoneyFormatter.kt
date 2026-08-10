package com.myexpense.tracker.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats amounts stored in minor units (cents) into readable strings.
 */
object MoneyFormatter {

    private val symbols = DecimalFormatSymbols.getInstance(Locale.US)

    private fun decimalFormat(maxFractionDigits: Int = 2): DecimalFormat =
        DecimalFormat("#,##0.##", symbols).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxFractionDigits
            roundingMode = RoundingMode.HALF_UP
        }

    /** 123456 → "1,234.56" (no symbol). */
    fun format(amountMinor: Long, maxFractionDigits: Int = 2): String {
        val value = BigDecimal(amountMinor).movePointLeft(2)
        return decimalFormat(maxFractionDigits).format(value)
    }

    /** 123456, "$" → "$1,234.56". */
    fun formatWithSymbol(amountMinor: Long, symbol: String): String =
        "$symbol${format(amountMinor)}"

    /** Formats an amount with sign, e.g. "+$12.00" / "-$12.00". */
    fun formatSigned(amountMinor: Long, symbol: String): String {
        val body = format(amountMinor)
        return if (amountMinor < 0) "-$symbol${body.removePrefix("-")}" else "+$symbol$body"
    }

    /**
     * Indian-style grouping ("₹1,24,500.00"): the last 3 digits group as one,
     * every group before it has 2 digits.
     */
    fun formatIndian(amountMinor: Long, symbol: String): String {
        val abs = kotlin.math.abs(amountMinor)
        val sign = if (amountMinor < 0) "-" else ""
        val rupees = abs / 100
        val paise = (abs % 100).toString().padStart(2, '0')
        val grouped = indianGroup(rupees.toString())
        return "$sign$symbol$grouped.$paise"
    }

    private fun indianGroup(digits: String): String {
        if (digits.length <= 3) return digits
        val last = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val groups = mutableListOf<String>()
        while (rest.length > 2) {
            groups.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) groups.add(0, rest)
        return groups.joinToString(",") + "," + last
    }

    /** Compact form for large balances: 1234567 → "1.23M". */
    fun formatCompact(amountMinor: Long, symbol: String): String {
        val abs = kotlin.math.abs(amountMinor)
        val sign = if (amountMinor < 0) "-" else ""
        val value = BigDecimal(abs).movePointLeft(2).toDouble()
        return when {
            value >= 1_000_000_000 -> sign + symbol + trim(decimalFormat(1).format(value / 1_000_000_000)) + "B"
            value >= 1_000_000 -> sign + symbol + trim(decimalFormat(1).format(value / 1_000_000)) + "M"
            value >= 10_000 -> sign + symbol + trim(decimalFormat(0).format(value / 1_000)) + "K"
            else -> sign + symbol + format(amountMinor)
        }
    }

    private fun trim(s: String): String =
        if (s.endsWith(".0")) s.dropLast(2) else s
}

/** Default currency symbols offered in settings. */
object CurrencySymbols {
    val symbols = listOf(
        "$", "€", "£", "¥", "₹", "₽", "₩", "₺", "R$", "A$", "C$", "S$", "HK$", "kr", "₴", "₦", "₱", "د.إ", "﷼", "zł", "Ft", "Kč", "CHF", "₪"
    )
}
