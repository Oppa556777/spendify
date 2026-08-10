package com.myexpense.tracker.data.model

import java.time.LocalDate

/** Time window filter used on the reports screen. */
enum class ReportsPeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    CUSTOM("Custom"),
}

/** One cell of the spending heatmap. */
data class HeatCell(
    val date: LocalDate,
    val expense: Long,      // minor units
    val level: Int,         // 0..4
)

/** One category's monthly expense trend. */
data class CategoryTrend(
    val name: String,
    val color: Long,
    val values: List<Long>, // minor units per month (oldest → newest)
)

/** Opening / income / expense / closing for the waterfall chart. */
data class CashFlow(
    val opening: Long,
    val income: Long,
    val expense: Long,
    val closing: Long,
)

/** One bucket of the income-vs-expense bar chart. */
data class BarBucket(
    val label: String,
    val income: Long,
    val expense: Long,
)

/** One point of the net-worth series. */
data class NetWorthPoint(
    val label: String,
    val value: Long,
)
