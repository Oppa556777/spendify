package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Seeds sensible defaults (categories + a cash account) on first launch. */
@Singleton
class SeedRepository @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun seedIfNeeded() {
        val firstRunDone = settingsRepository.settings.first().firstRunComplete
        if (firstRunDone) return

        if (categoryRepository.getAll().isEmpty()) {
            categoryRepository.insertAll(defaultExpenseCategories + defaultIncomeCategories)
        }
        if (accountRepository.getActive().isEmpty()) {
            accountRepository.save(
                Account(
                    name = "Cash",
                    type = AccountType.CASH,
                    initialBalance = 0,
                    color = 0xFF4CAF50,
                    icon = "payments",
                )
            )
        }
        settingsRepository.setFirstRunComplete()
    }

    companion object {
        val defaultExpenseCategories: List<Category> = listOf(
            Category(name = "Food & Dining", type = TransactionType.EXPENSE, icon = "restaurant", color = 0xFFEF5350, isDefault = true, sortOrder = 1),
            Category(name = "Groceries", type = TransactionType.EXPENSE, icon = "shopping_cart", color = 0xFFFFA726, isDefault = true, sortOrder = 2),
            Category(name = "Transport", type = TransactionType.EXPENSE, icon = "directions_bus", color = 0xFF42A5F5, isDefault = true, sortOrder = 3),
            Category(name = "Fuel", type = TransactionType.EXPENSE, icon = "local_gas_station", color = 0xFF26A69A, isDefault = true, sortOrder = 4),
            Category(name = "Shopping", type = TransactionType.EXPENSE, icon = "shopping_bag", color = 0xFFAB47BC, isDefault = true, sortOrder = 5),
            Category(name = "Entertainment", type = TransactionType.EXPENSE, icon = "movie", color = 0xFFEC407A, isDefault = true, sortOrder = 6),
            Category(name = "Health", type = TransactionType.EXPENSE, icon = "medical_services", color = 0xFFEF5350, isDefault = true, sortOrder = 7),
            Category(name = "Housing", type = TransactionType.EXPENSE, icon = "home", color = 0xFF8D6E63, isDefault = true, sortOrder = 8),
            Category(name = "Utilities", type = TransactionType.EXPENSE, icon = "bolt", color = 0xFFFFCA28, isDefault = true, sortOrder = 9),
            Category(name = "Education", type = TransactionType.EXPENSE, icon = "school", color = 0xFF5C6BC0, isDefault = true, sortOrder = 10),
            Category(name = "Travel", type = TransactionType.EXPENSE, icon = "flight", color = 0xFF29B6F6, isDefault = true, sortOrder = 11),
            Category(name = "Bills & Fees", type = TransactionType.EXPENSE, icon = "receipt_long", color = 0xFF78909C, isDefault = true, sortOrder = 12),
            Category(name = "Other", type = TransactionType.EXPENSE, icon = "category", color = 0xFF9E9E9E, isDefault = true, sortOrder = 99),
        )

        val defaultIncomeCategories: List<Category> = listOf(
            Category(name = "Salary", type = TransactionType.INCOME, icon = "payments", color = 0xFF66BB6A, isDefault = true, sortOrder = 1),
            Category(name = "Business", type = TransactionType.INCOME, icon = "work", color = 0xFF26A69A, isDefault = true, sortOrder = 2),
            Category(name = "Freelance", type = TransactionType.INCOME, icon = "laptop", color = 0xFF42A5F5, isDefault = true, sortOrder = 3),
            Category(name = "Investments", type = TransactionType.INCOME, icon = "trending_up", color = 0xFFAB47BC, isDefault = true, sortOrder = 4),
            Category(name = "Gifts", type = TransactionType.INCOME, icon = "redeem", color = 0xFFEC407A, isDefault = true, sortOrder = 5),
            Category(name = "Other Income", type = TransactionType.INCOME, icon = "add_circle", color = 0xFF9E9E9E, isDefault = true, sortOrder = 99),
        )
    }
}
