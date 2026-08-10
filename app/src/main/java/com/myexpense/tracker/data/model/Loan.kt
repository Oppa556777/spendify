package com.myexpense.tracker.data.model

/** A loan between the user and another person. */
data class Loan(
    val id: Long = 0,
    val type: LoanType = LoanType.LENT,
    val personName: String,
    val amount: Long = 0,           // minor units
    val paidAmount: Long = 0,       // minor units
    val date: Long = 0,             // loan date (epoch millis)
    val dueDate: Long? = null,      // epoch millis
    val note: String? = null,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class LoanStatus { ACTIVE, OVERDUE, SETTLED }

val Loan.remaining: Long get() = (amount - paidAmount).coerceAtLeast(0)

val Loan.status: LoanStatus
    get() = when {
        isSettled -> LoanStatus.SETTLED
        dueDate != null && dueDate < System.currentTimeMillis() -> LoanStatus.OVERDUE
        else -> LoanStatus.ACTIVE
    }
