package com.myexpense.tracker.data.model

import java.time.LocalDate

/** Time window filter used on the home dashboard. */
enum class HomePeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom"),
}

/** One day of the 7-day spending overview chart. */
data class DailyStat(
    val date: LocalDate,
    val income: Long,     // minor units
    val expense: Long,    // minor units
)

/** A subscription whose next due date falls inside the reminder window. */
data class SubscriptionReminder(
    val id: Long,
    val name: String,
    val amount: Long,             // minor units
    val nextDueDate: Long,
    val daysLeft: Int,
    val color: Long,
    val icon: String,
    val billingCycle: BillingCycle,
)
