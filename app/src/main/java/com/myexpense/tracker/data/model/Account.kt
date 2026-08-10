package com.myexpense.tracker.data.model

/** A place where money lives (bank account, cash, wallet, card…). */
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val balance: Long = 0,                  // minor units (cents)
    val currency: String = "INR",
    val color: Long = 0xFF3F51B5,           // opaque ARGB
    val icon: String = "account_balance_wallet",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
