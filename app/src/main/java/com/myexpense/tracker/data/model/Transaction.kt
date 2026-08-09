package com.myexpense.tracker.data.model

import java.time.LocalDate

/** A single money movement: an expense or an income. */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,            // stored in minor units (cents)
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val date: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}
