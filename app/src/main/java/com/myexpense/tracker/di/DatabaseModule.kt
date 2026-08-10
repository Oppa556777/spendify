package com.myexpense.tracker.di

import android.content.Context
import androidx.room.Room
import com.myexpense.tracker.data.database.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "moneymate.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideLoanDao(db: AppDatabase): LoanDao = db.loanDao()

    @Provides
    fun provideSubscriptionDao(db: AppDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides
    fun provideRecurringRuleDao(db: AppDatabase): RecurringRuleDao = db.recurringRuleDao()

    @Provides
    fun provideBillSplitDao(db: AppDatabase): BillSplitDao = db.billSplitDao()

    @Provides
    fun provideBillSplitMemberDao(db: AppDatabase): BillSplitMemberDao = db.billSplitMemberDao()

    @Provides
    fun provideAssetDao(db: AppDatabase): AssetDao = db.assetDao()

    @Provides
    fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideAppSettingDao(db: AppDatabase): AppSettingDao = db.appSettingDao()
}
