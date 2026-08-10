package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.AchievementDao
import com.myexpense.tracker.data.database.dao.AppSettingDao
import com.myexpense.tracker.data.database.entity.AppSettingEntity
import com.myexpense.tracker.data.database.entity.AchievementEntity
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.utils.NotificationHelper
import com.myexpense.tracker.utils.endMillis
import com.myexpense.tracker.utils.startMillis
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks achievement conditions after every relevant action and unlocks
 * matching achievements, showing a notification for each unlock.
 *
 * Streak tracking ("Week Warrior"/"Month Master") and the 90-day "Consistent"
 * achievement are persisted in the app_settings key-value table.
 */
@Singleton
class AchievementUnlocker @Inject constructor(
    private val achievementDao: AchievementDao,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val loanRepository: LoanRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val tagRepository: TagRepository,
    private val appSettingDao: AppSettingDao,
    private val notificationHelper: NotificationHelper,
) {

    // ── Streak persistence (app_settings) ──────────────────────────────────
    private suspend fun getSetting(key: String, default: String): String =
        appSettingDao.getValue(key) ?: default

    private suspend fun setSetting(key: String, value: String) =
        appSettingDao.upsert(AppSettingEntity(key, value))

    /** Called on app open and after every transaction: updates streak + first-open. */
    suspend fun onAppOpen() {
        val today = LocalDate.now().toString()
        val firstOpen = getSetting("first_open", "")
        if (firstOpen.isBlank()) setSetting("first_open", System.currentTimeMillis().toString())
        updateStreak(today)
        checkAll()
    }

    suspend fun onTransactionAdded(t: Transaction) {
        updateStreak(LocalDate.now().toString())
        checkAll()
    }

    private suspend fun updateStreak(todayIso: String) {
        val lastActive = getSetting("last_active", "")
        val streak = getSetting("streak_days", "0").toIntOrNull() ?: 0
        val newStreak = when {
            lastActive == todayIso -> streak
            lastActive == LocalDate.now().minusDays(1).toString() -> streak + 1
            else -> 1
        }
        setSetting("last_active", todayIso)
        setSetting("streak_days", newStreak.toString())
    }

    suspend fun onBudgetCreated() = checkAll()
    suspend fun onGoalSaved() = checkAll()
    suspend fun onAccountSaved() = checkAll()
    suspend fun onLoanSettled() = checkAll()
    suspend fun onSubscriptionSaved() = checkAll()
    suspend fun onTagCreated() = checkAll()
    suspend fun onExport() = checkAll()

    // ── The engine ─────────────────────────────────────────────────────────
    private suspend fun checkAll() {
        val achievements = achievementDao.getAll()
        val unlockedIds = achievements.filter { it.isUnlocked }.map { it.id }.toSet()
        val newlyUnlocked = mutableListOf<AchievementEntity>()
        val now = System.currentTimeMillis()

        fun unlock(type: String) {
            achievements.firstOrNull { it.type == type && !it.isUnlocked }?.let {
                achievementDao.markUnlocked(it.id, now)
                newlyUnlocked += it
            }
        }

        // Counts & state
        val txCount = transactionRepository.observeAll().first().size
        val receiptCount = transactionRepository.observeAll().first().count { it.receiptImagePath != null }
        val budgets = budgetRepository.observeBudgetsWithSpent().first()
        val goals = goalRepository.observeAll().first()
        val accounts = accountRepository.getActive()
        val loans = loanRepository.observeAll().first()
        val subs = subscriptionRepository.observeAllWithCategories().first()
        val tags = tagRepository.getAll()

        // First Step
        if (txCount >= 1) unlock("TRANSACTION")
        // Century Club
        if (txCount >= 100) unlock("COUNT_100")
        // Detail Oriented
        if (receiptCount >= 10) unlock("RECEIPT_10")
        // Budget Master
        if (budgets.isNotEmpty()) unlock("BUDGET")
        // Saver
        if (goals.isNotEmpty()) unlock("GOAL")
        // Goal Crusher
        if (goals.any { it.goal.isCompleted }) unlock("GOAL_COMPLETE")
        // Multi-Banker
        if (accounts.size >= 3) unlock("ACCOUNT_3")
        // Loan Free
        if (loans.isNotEmpty() && loans.none { !it.isSettled }) unlock("LOAN_FREE")
        // Subscription Guru
        if (subs.size >= 5) unlock("SUBSCRIPTION_5")
        // Tag Team
        if (tags.size >= 5) unlock("TAG_5")

        // Streak based
        val streak = getSetting("streak_days", "0").toIntOrNull() ?: 0
        if (streak >= 7) unlock("STREAK_7")
        if (streak >= 30) unlock("STREAK_30")

        // Consistent: 90 days since first open
        val firstOpen = getSetting("first_open", "").toLongOrNull()
        if (firstOpen != null && now - firstOpen >= 90L * 86_400_000L) unlock("CONSISTENT")

        // No Overspend: all budgets within limit this month
        val month = YearMonth.now()
        if (budgets.isNotEmpty() && budgets.all { it.spent <= it.budget.limitAmount }) {
            unlock("BUDGET_OK")
        }

        // Penny Pincher: month expense < 50% of income
        val income = transactionRepository.observeIncomeForMonth(month).first()
        val expense = transactionRepository.observeExpenseForMonth(month).first()
        if (income > 0 && expense * 2 < income) unlock("PENNY")

        // Money Master: all other achievements unlocked
        val others = achievements.filter { it.type != "MASTER" }
        if (others.isNotEmpty() && others.all { it.isUnlocked || it.type == "MASTER" }) {
            unlock("MASTER")
        }

        // Notify
        newlyUnlocked.forEach { notificationHelper.showAchievementUnlocked(it.title) }
    }

    /** Called from the transaction form with the freshly saved transaction. */
    suspend fun onTransactionSaved(transaction: Transaction) {
        val hour = runCatching {
            transaction.time.substring(0, 2).toIntOrNull() ?: -1
        }.getOrDefault(-1)
        when {
            hour in 0..4 -> unlockType("NIGHT")
            hour in 5..6 -> unlockType("EARLY")
        }
        if (transaction.isExpense && transaction.amount >= 10_000L * 100) {
            unlockType("BIG_SPEND")
        }
        onTransactionAdded(transaction)
    }

    private suspend fun unlockType(type: String) {
        val now = System.currentTimeMillis()
        achievementDao.getAll().firstOrNull { it.type == type && !it.isUnlocked }?.let {
            achievementDao.markUnlocked(it.id, now)
            notificationHelper.showAchievementUnlocked(it.title)
        }
    }
}
