package com.myexpense.tracker.navigation

import com.myexpense.tracker.data.model.TransactionType

/** All navigation destinations. */
object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val STATS = "stats"
    const val BUDGETS = "budgets"
    const val SETTINGS = "settings"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val BACKUP = "backup"
    const val ADD_TRANSACTION = "transaction/add?type={type}"
    const val EDIT_TRANSACTION = "transaction/edit/{id}"
    const val ONBOARDING = "onboarding"

    fun addTransaction(type: TransactionType): String = "transaction/add?type=${type.name}"
    fun editTransaction(id: Long) = "transaction/edit/$id"
}
