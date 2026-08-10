package com.myexpense.tracker.data.model

/** How often a budget repeats. */
enum class BudgetPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

/** Human-readable label for budget periods. */
val BudgetPeriod.label: String
    get() = when (this) {
        BudgetPeriod.DAILY -> "Daily"
        BudgetPeriod.WEEKLY -> "Weekly"
        BudgetPeriod.MONTHLY -> "Monthly"
        BudgetPeriod.YEARLY -> "Yearly"
        BudgetPeriod.CUSTOM -> "Custom"
    }

/** Direction of a loan from the user's perspective. */
enum class LoanType { LENT, BORROWED }

/** Subscription billing cycles. */
enum class BillingCycle { WEEKLY, MONTHLY, YEARLY }

/** Recurring transaction frequencies. */
enum class RecurringFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/** Asset classes tracked in the portfolio. */
enum class AssetType { STOCK, MUTUAL_FUND, CRYPTO, REAL_ESTATE, GOLD, FD, OTHER }
