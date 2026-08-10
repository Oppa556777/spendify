package com.myexpense.tracker.data.model

/** How often a budget repeats. */
enum class BudgetPeriod { WEEKLY, MONTHLY, YEARLY, CUSTOM }

/** Direction of a loan from the user's perspective. */
enum class LoanType { LENT, BORROWED }

/** Subscription billing cycles. */
enum class BillingCycle { WEEKLY, MONTHLY, YEARLY }

/** Recurring transaction frequencies. */
enum class RecurringFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/** Asset classes tracked in the portfolio. */
enum class AssetType { STOCK, MUTUAL_FUND, CRYPTO, REAL_ESTATE, GOLD, FD, OTHER }
