package com.myexpense.tracker.db

/** Domain models (framework-only build, no Room). */

enum class TxType { EXPENSE, INCOME }

enum class AccountType { CASH, BANK, CARD, E_WALLET, INVESTMENT, OTHER }

data class Category(
    val id: Long = 0,
    val name: String,
    val type: TxType,
    val icon: String = "\uD83D\uDCC1", // emoji
    val color: Int = 0xFF4CAF50.toInt(),
    val sortOrder: Int = 0
)

data class Transaction(
    val id: Long = 0,
    val type: TxType,
    val amount: Long,          // minor units (cents)
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val date: String = ""  // ISO yyyy-MM-dd
)

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val initialBalance: Long = 0,
    val color: Int = 0xFF3F51B5.toInt(),
    val icon: String = "\uD83D\uDCB3"
)

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val amount: Long,
    val month: String? = null,   // ISO yyyy-MM or null = recurring
    val isRecurring: Boolean = true
)

data class BudgetRow(
    val budget: Budget,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Int,
    val spent: Long
) {
    val progress: Float
        get() = if (budget.amount <= 0) 0f else (spent.toFloat() / budget.amount).coerceIn(0f, 1f)
    val overspent: Boolean get() = spent > budget.amount
}

data class CategoryStat(
    val categoryId: Long?,
    val total: Long,
    val count: Int,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Int
)
