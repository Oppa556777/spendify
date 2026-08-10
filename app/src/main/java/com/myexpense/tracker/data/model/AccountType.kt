package com.myexpense.tracker.data.model

/** Kind of account the user tracks money in. */
enum class AccountType {
    BANK,
    CASH,
    CREDIT_CARD,
    WALLET,
    SAVINGS,
    INVESTMENT,
    OTHER
}

/** Display emoji per account type (used in the accounts UI). */
val AccountType.emoji: String
    get() = when (this) {
        AccountType.BANK -> "🏦"
        AccountType.CASH -> "💵"
        AccountType.CREDIT_CARD -> "💳"
        AccountType.WALLET -> "👛"
        AccountType.SAVINGS -> "💰"
        AccountType.INVESTMENT -> "📈"
        AccountType.OTHER -> "🏠"
    }
