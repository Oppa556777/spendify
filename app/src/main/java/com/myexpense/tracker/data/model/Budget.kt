package com.myexpense.tracker.data.model

/**
 * A spending limit for a category (or all categories when [categoryId] is null)
 * over a [period].
 */
data class Budget(
    val id: Long = 0,
    val name: String = "",
    val categoryId: Long? = null,           // null = all categories
    val limitAmount: Long = 0,              // minor units
    val spentAmount: Long = 0,              // cached/calculated
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val colorHex: String = "#4CAF50",
    val alertAt: Int = 80,                  // alert when spent reaches this %
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Budget joined with its category and how much was already spent. */
data class BudgetWithSpent(
    val budget: Budget,
    val category: Category?,
    val spent: Long,
    val income: Long = 0,
) {
    val progress: Float
        get() = if (budget.limitAmount <= 0L) 0f
        else (spent.toFloat() / budget.limitAmount.toFloat()).coerceIn(0f, 1f)
    val remaining: Long get() = budget.limitAmount - spent
    val isOverspent: Boolean get() = spent > budget.limitAmount
}
