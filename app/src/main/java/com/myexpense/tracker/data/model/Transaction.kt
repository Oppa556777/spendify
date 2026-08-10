package com.myexpense.tracker.data.model

import java.time.LocalDate

/**
 * A single money movement: an expense, income or transfer.
 *
 * Amounts are stored in the database as [Double] rupees but the domain layer
 * uses minor units (cents) as [Long] to avoid floating point drift.
 */
data class Transaction(
    val id: Long = 0,
    val title: String = "",
    val amount: Long,               // minor units (cents)
    val type: TransactionType,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,  // set for TRANSFER transactions only
    val note: String = "",
    val date: LocalDate,
    val time: String = "",          // "HH:mm"
    val tags: String? = null,       // comma-separated tag IDs
    val personId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val receiptImagePath: String? = null,
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}
