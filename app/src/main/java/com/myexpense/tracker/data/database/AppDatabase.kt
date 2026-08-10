package com.myexpense.tracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myexpense.tracker.data.database.dao.AccountDao
import com.myexpense.tracker.data.database.dao.AchievementDao
import com.myexpense.tracker.data.database.dao.AppSettingDao
import com.myexpense.tracker.data.database.dao.AssetDao
import com.myexpense.tracker.data.database.dao.BillSplitDao
import com.myexpense.tracker.data.database.dao.BillSplitMemberDao
import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.GoalDao
import com.myexpense.tracker.data.database.dao.LoanDao
import com.myexpense.tracker.data.database.dao.PersonDao
import com.myexpense.tracker.data.database.dao.RecurringRuleDao
import com.myexpense.tracker.data.database.dao.SubscriptionDao
import com.myexpense.tracker.data.database.dao.TagDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.AccountEntity
import com.myexpense.tracker.data.database.entity.AchievementEntity
import com.myexpense.tracker.data.database.entity.AppSettingEntity
import com.myexpense.tracker.data.database.entity.AssetEntity
import com.myexpense.tracker.data.database.entity.BillSplitEntity
import com.myexpense.tracker.data.database.entity.BillSplitMemberEntity
import com.myexpense.tracker.data.database.entity.BudgetCategoryEntity
import com.myexpense.tracker.data.database.entity.BudgetEntity
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.database.entity.GoalEntity
import com.myexpense.tracker.data.database.entity.LoanEntity
import com.myexpense.tracker.data.database.entity.PersonEntity
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import com.myexpense.tracker.data.database.entity.SubscriptionEntity
import com.myexpense.tracker.data.database.entity.TagEntity
import com.myexpense.tracker.data.database.entity.TransactionEntity

/**
 * MoneyMate database — 15 tables:
 * accounts, categories, transactions, budgets, goals, loans, subscriptions,
 * tags, people, recurring_rules, bill_splits, bill_split_members, assets,
 * achievements, app_settings.
 *
 * All dates are stored as epoch-millisecond timestamps; enums are stored as
 * their names via Room's built-in enum converters.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BudgetCategoryEntity::class,
        GoalEntity::class,
        LoanEntity::class,
        SubscriptionEntity::class,
        TagEntity::class,
        PersonEntity::class,
        RecurringRuleEntity::class,
        BillSplitEntity::class,
        BillSplitMemberEntity::class,
        AssetEntity::class,
        AchievementEntity::class,
        AppSettingEntity::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun loanDao(): LoanDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun tagDao(): TagDao
    abstract fun personDao(): PersonDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun billSplitDao(): BillSplitDao
    abstract fun billSplitMemberDao(): BillSplitMemberDao
    abstract fun assetDao(): AssetDao
    abstract fun achievementDao(): AchievementDao
    abstract fun appSettingDao(): AppSettingDao
}
