package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.AchievementDao
import com.myexpense.tracker.data.database.entity.AchievementEntity
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds sensible defaults on first launch:
 * - the 16 default EXPENSE + 8 default INCOME categories
 * - the sample achievements (gamification)
 *
 * The primary account is created by the onboarding setup sheet instead, so the
 * user can name it and set its starting balance themselves.
 */
@Singleton
class SeedRepository @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val achievementDao: AchievementDao,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun seedIfNeeded() {
        // Re-seed if the tables are empty (e.g. after a destructive migration),
        // even when the DataStore flag was already set.
        if (categoryRepository.getAll().isEmpty()) {
            categoryRepository.insertAll(defaultCategories())
        }
        if (achievementDao.getAll().isEmpty()) {
            achievementDao.insertAll(sampleAchievements())
        }
        if (!settingsRepository.settings.first().firstRunComplete) {
            settingsRepository.setSeedingDone()
        }
    }

    companion object {

        private fun category(name: String, type: TransactionType, icon: String, color: Long) =
            Category(name = name, type = type, icon = icon, color = color, isDefault = true)

        fun defaultCategories(): List<Category> = listOf(
            // ── Expense (16) ────────────────────────────────────────────────
            category("Food & Dining", TransactionType.EXPENSE, "restaurant", 0xFFEF5350),
            category("Transport", TransactionType.EXPENSE, "directions_bus", 0xFF42A5F5),
            category("Shopping", TransactionType.EXPENSE, "shopping_bag", 0xFFAB47BC),
            category("Entertainment", TransactionType.EXPENSE, "movie", 0xFFEC407A),
            category("Health", TransactionType.EXPENSE, "medical_services", 0xFF26A69A),
            category("Education", TransactionType.EXPENSE, "school", 0xFF5C6BC0),
            category("Bills & Utilities", TransactionType.EXPENSE, "bolt", 0xFFFFCA28),
            category("Housing/Rent", TransactionType.EXPENSE, "home", 0xFF8D6E63),
            category("Travel", TransactionType.EXPENSE, "flight", 0xFF29B6F6),
            category("Personal Care", TransactionType.EXPENSE, "spa", 0xFFF06292),
            category("Gifts", TransactionType.EXPENSE, "card_giftcard", 0xFFD81B60),
            category("Pets", TransactionType.EXPENSE, "pets", 0xFFA1887F),
            category("Insurance", TransactionType.EXPENSE, "workspace_premium", 0xFF78909C),
            category("Investments", TransactionType.EXPENSE, "trending_up", 0xFF66BB6A),
            category("Subscriptions", TransactionType.EXPENSE, "credit_card", 0xFF7E57C2),
            category("Others", TransactionType.EXPENSE, "category", 0xFF9E9E9E),
            // ── Income (8) ──────────────────────────────────────────────────
            category("Salary", TransactionType.INCOME, "payments", 0xFF43A047),
            category("Freelance", TransactionType.INCOME, "laptop", 0xFF26A69A),
            category("Business", TransactionType.INCOME, "work", 0xFF3949AB),
            category("Investment Returns", TransactionType.INCOME, "trending_up", 0xFF66BB6A),
            category("Rental Income", TransactionType.INCOME, "home", 0xFF8D6E63),
            category("Gift Received", TransactionType.INCOME, "redeem", 0xFFEC407A),
            category("Bonus", TransactionType.INCOME, "emoji_events", 0xFFFFB300),
            category("Others", TransactionType.INCOME, "add_circle", 0xFF9E9E9E),
        )

        /** The 20 built-in achievements (all free, no paywall). */
        fun sampleAchievements(): List<AchievementEntity> {
            fun a(title: String, description: String, icon: String, type: String) =
                AchievementEntity(title = title, description = description, iconName = icon, type = type)
            return listOf(
                a("First Step", "Add your first transaction", "payments", "TRANSACTION"),
                a("Budget Master", "Create your first budget", "savings", "BUDGET"),
                a("Saver", "Create your first savings goal", "star", "GOAL"),
                a("Week Warrior", "Track expenses 7 days in a row", "local_fire_department", "STREAK_7"),
                a("Month Master", "Track expenses 30 days in a row", "emoji_events", "STREAK_30"),
                a("Century Club", "Log 100 transactions", "100", "COUNT_100"),
                a("No Overspend", "Stay within all budgets for a month", "verified", "BUDGET_OK"),
                a("Goal Crusher", "Complete a savings goal", "military_tech", "GOAL_COMPLETE"),
                a("Detail Oriented", "Add 10 transactions with receipts", "receipt_long", "RECEIPT_10"),
                a("Tag Team", "Create 5 custom tags", "sell", "TAG_5"),
                a("Multi-Banker", "Add 3 or more accounts", "account_balance", "ACCOUNT_3"),
                a("Loan Free", "Settle all your loans", "payments", "LOAN_FREE"),
                a("Subscription Guru", "Track 5 or more subscriptions", "subscriptions", "SUBSCRIPTION_5"),
                a("Export Pro", "Export a report (PDF or CSV)", "picture_as_pdf", "EXPORT"),
                a("Night Owl", "Add a transaction after midnight", "nights_stay", "NIGHT"),
                a("Early Bird", "Add a transaction before 7 AM", "wb_sunny", "EARLY"),
                a("Big Spender", "A single expense over ₹10,000", "local_fire_department", "BIG_SPEND"),
                a("Penny Pincher", "Monthly expenses under 50% of income", "savings", "PENNY"),
                a("Consistent", "Use the app for 90 days", "calendar_month", "CONSISTENT"),
                a("Money Master", "Unlock all other achievements", "workspace_premium", "MASTER"),
            )
        }
    }
}