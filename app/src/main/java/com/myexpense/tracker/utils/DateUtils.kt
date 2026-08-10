package com.myexpense.tracker.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

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

// ── Timestamp conversions (database stores epoch millis, UTC midnight) ──────

fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Start of month as epoch millis (UTC midnight of day 1). */
fun YearMonth.startMillis(): Long = atDay(1).toEpochMillis()

/** End of month as epoch millis (UTC midnight of the last day — inclusive). */
fun YearMonth.endMillis(): Long = atEndOfMonth().toEpochMillis()

/** Double rupees (database) → minor units (domain). */
fun Double.toMinorUnits(): Long = (this * 100).roundToLong()

/** Minor units (domain) → Double rupees (database). */
fun Long.toRupees(): Double = this / 100.0
