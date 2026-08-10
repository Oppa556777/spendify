package com.myexpense.tracker.data.model

import java.time.YearMonth

/**
 * A spending limit over one or more categories ([categoryIds]) or all
 * expenses ([categoryId] == null) for a [period].
 */
data class Budget(
    val id: Long = 0,
    val name: String = "",
    val categoryId: Long? = null,               // null = all expenses; otherwise the primary category
    val categoryIds: List<Long> = emptyList(),  // full category set (multi-category budgets)
    val limitAmount: Long = 0,                  // minor units
    val spentAmount: Long = 0,                  // cached/calculated
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val colorHex: String = "#4CAF50",
    val alertAt: Int = 80,                      // alert when spent reaches this %
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Derived budget status used by the overview filter. */
enum class BudgetStatus { ACTIVE, OVER_BUDGET, COMPLETED }

/** Budget joined with its display category and how much was spent. */
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

    val status: BudgetStatus
        get() = when {
            !budget.isActive -> BudgetStatus.COMPLETED
            isOverspent -> BudgetStatus.OVER_BUDGET
            else -> BudgetStatus.ACTIVE
        }
}
