package com.myexpense.tracker.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    private val monthYearFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

    fun monthYear(month: YearMonth): String = month.format(monthYearFormat)

    fun shortMonthYear(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault()))

    fun shortDate(date: LocalDate): String = date.format(shortDate)

    fun fullDate(date: LocalDate): String = date.format(fullDate)

    fun dayOfWeek(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun today(): LocalDate = LocalDate.now()

    fun parseMonthYear(text: String): YearMonth = YearMonth.parse(text, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    /** Whether the date belongs to the given month. */
    fun inMonth(date: LocalDate, month: YearMonth): Boolean =
        date.year == month.year && date.monthValue == month.monthValue
}
