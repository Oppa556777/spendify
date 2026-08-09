package com.myexpense.tracker.data.model

import java.time.YearMonth

/** A monthly limit assigned to a category. */
data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val amount: Long,          // monthly limit, minor units
    val month: YearMonth? = null, // null → recurring budget
    val isRecurring: Boolean = true,
)

/** Budget joined with the category it belongs to and how much was already spent. */
data class BudgetWithSpent(
    val budget: Budget,
    val category: Category?,
    val spent: Long,
    val income: Long,
) {
    val progress: Float
        get() = if (budget.amount <= 0L) 0f else (spent.toFloat() / budget.amount.toFloat()).coerceIn(0f, 1f)
    val remaining: Long get() = budget.amount - spent
    val isOverspent: Boolean get() = spent > budget.amount
}
