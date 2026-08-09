package com.myexpense.tracker.data.model

/** Aggregated spending per category for a period. */
data class CategoryStat(
    val categoryId: Long?,
    val total: Long,
    val count: Int,
    val fraction: Float = 0f,
) {
    var category: Category? = null
        internal set

    val label: String get() = category?.name ?: "Uncategorized"
}

/** One point of a monthly trend chart. */
data class MonthlyPoint(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
)

/** Account joined with its live balance. */
data class AccountWithBalance(
    val account: Account,
    val balance: Long,
)
