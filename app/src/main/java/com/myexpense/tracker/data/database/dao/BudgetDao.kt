package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @androidx.room.Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isRecurring = 1 OR month = :month ORDER BY amount DESC")
    fun observeForMonth(month: YearMonth): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
