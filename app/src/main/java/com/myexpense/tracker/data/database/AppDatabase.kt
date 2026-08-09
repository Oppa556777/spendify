package com.myexpense.tracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.myexpense.tracker.data.database.dao.AccountDao
import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.AccountEntity
import com.myexpense.tracker.data.database.entity.BudgetEntity
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.database.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromYearMonth(value: YearMonth?): String? = value?.toString()

    @TypeConverter
    fun toYearMonth(value: String?): YearMonth? = value?.let(YearMonth::parse)
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
}
