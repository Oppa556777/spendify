package com.myexpense.tracker.data.model

/** A place where money lives (cash, bank account, card, wallet…). */
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val initialBalance: Long = 0,   // minor units
    val color: Long = 0xFF3F51B5,
    val icon: String = "account_balance_wallet",
    val isArchived: Boolean = false,
)
