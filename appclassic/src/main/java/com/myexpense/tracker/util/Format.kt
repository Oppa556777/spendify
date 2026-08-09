package com.myexpense.tracker.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {

    fun money(amountMinor: Long, symbol: String): String {
        val value = BigDecimal(amountMinor).movePointLeft(2)
        val df = DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(Locale.US))
        df.minimumFractionDigits = 0
        df.maximumFractionDigits = 2
        return symbol + df.format(value)
    }

    fun signed(amountMinor: Long, symbol: String): String {
        val body = money(kotlin.math.abs(amountMinor), symbol)
        return if (amountMinor < 0) "-$body" else "+$body"
    }

    fun monthYear(month: String): String {
        // yyyy-MM -> "June 2026"
        val parts = month.split("-")
        if (parts.size != 2) return month
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, parts[0].toInt())
        cal.set(Calendar.MONTH, parts[1].toInt() - 1)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun shortMonth(month: String): String {
        val parts = month.split("-")
        if (parts.size != 2) return month
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, parts[0].toInt())
        cal.set(Calendar.MONTH, parts[1].toInt() - 1)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }

    fun fullDate(dateIso: String): String {
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: return dateIso
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(d)
        } catch (e: Exception) {
            dateIso
        }
    }

    fun shortDate(dateIso: String): String {
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: return dateIso
            SimpleDateFormat("d MMM", Locale.getDefault()).format(d)
        } catch (e: Exception) {
            dateIso
        }
    }

    fun addMonths(month: String, delta: Int): String {
        val parts = month.split("-")
        val y = parts[0].toInt()
        var m = parts[1].toInt() + delta
        var yy = y
        while (m <= 0) { m += 12; yy-- }
        while (m > 12) { m -= 12; yy++ }
        return String.format(Locale.US, "%04d-%02d", yy, m)
    }
}

object CurrencySymbols {
    val all = listOf(
        "$", "€", "£", "¥", "₹", "₽", "₩", "₺", "R$", "A$", "C$", "S$", "HK$",
        "kr", "₴", "₦", "₱", "zł", "Ft", "Kč", "CHF", "₪"
    )
}
