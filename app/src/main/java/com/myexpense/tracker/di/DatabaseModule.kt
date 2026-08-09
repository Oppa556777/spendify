package com.myexpense.tracker.di

import android.content.Context
import androidx.room.Room
import com.myexpense.tracker.data.database.AppDatabase
import com.myexpense.tracker.data.database.dao.AccountDao
import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
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
}
