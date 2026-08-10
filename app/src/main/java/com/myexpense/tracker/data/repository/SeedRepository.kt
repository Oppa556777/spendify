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
        val firstRunDone = settingsRepository.settings.first().firstRunComplete
        if (firstRunDone) return

        if (categoryRepository.getAll().isEmpty()) {
            categoryRepository.insertAll(defaultCategories())
        }
        if (achievementDao.getAll().isEmpty()) {
            achievementDao.insertAll(sampleAchievements())
        }
        settingsRepository.setSeedingDone()
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

        /** Sample achievements, unlocked progressively as the user uses the app. */
        fun sampleAchievements(): List<AchievementEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                AchievementEntity(
                    title = "Welcome Aboard",
                    description = "You installed MoneyMate – a fully offline expense tracker.",
                    iconName = "workspace_premium",
                    isUnlocked = true,
                    unlockedAt = now,
                    type = "ONBOARDING",
                ),
                AchievementEntity(
                    title = "First Transaction",
                    description = "Add your very first expense or income.",
                    iconName = "payments",
                    type = "TRANSACTION",
                ),
                AchievementEntity(
                    title = "Budget Planner",
                    description = "Create your first monthly budget.",
                    iconName = "savings",
                    type = "BUDGET",
                ),
                AchievementEntity(
                    title = "Goal Setter",
                    description = "Create your first savings goal.",
                    iconName = "star",
                    type = "GOAL",
                ),
                AchievementEntity(
                    title = "Track Star",
                    description = "Log 50 transactions.",
                    iconName = "show_chart",
                    type = "TRANSACTION",
                ),
                AchievementEntity(
                    title = "Saver",
                    description = "Put aside money towards a goal.",
                    iconName = "savings",
                    type = "GOAL",
                ),
                AchievementEntity(
                    title = "Debt Free",
                    description = "Settle your first loan.",
                    iconName = "payments",
                    type = "LOAN",
                ),
                AchievementEntity(
                    title = "Early Bird",
                    description = "Log a transaction before 8 AM.",
                    iconName = "auto_awesome",
                    type = "TRANSACTION",
                ),
            )
        }
    }
}
