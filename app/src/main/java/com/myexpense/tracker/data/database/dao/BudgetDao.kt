package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/** Budget + live spent/remaining/progress for a period, computed in SQL. */
data class BudgetStatusRow(
    val id: Long,
    val name: String,
    val limitAmount: Double,
    val spent: Double,
    val remaining: Double,
    val progressPercent: Double,
)

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

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isActive = 1")
    suspend fun getActive(): List<BudgetEntity>

    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    /** Spent for one budget between dates (categoryId null = all categories). */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = 'EXPENSE' AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "AND date BETWEEN :from AND :to"
    )
    fun observeSpent(categoryId: Long?, from: Long, to: Long): Flow<Double>

    /** Budget remaining calculations for all active budgets over a period. */
    @Query(
        """
        SELECT b.id AS id,
               b.name AS name,
               b.limitAmount AS limitAmount,
               COALESCE((
                   SELECT SUM(t.amount) FROM transactions t
                   WHERE t.type = 'EXPENSE' AND t.date BETWEEN :from AND :to
                     AND (b.categoryId IS NULL OR t.categoryId = b.categoryId)
               ), 0) AS spent,
               b.limitAmount - COALESCE((
                   SELECT SUM(t.amount) FROM transactions t
                   WHERE t.type = 'EXPENSE' AND t.date BETWEEN :from AND :to
                     AND (b.categoryId IS NULL OR t.categoryId = b.categoryId)
               ), 0) AS remaining,
               CASE WHEN b.limitAmount > 0 THEN
                   COALESCE((
                       SELECT SUM(t.amount) FROM transactions t
                       WHERE t.type = 'EXPENSE' AND t.date BETWEEN :from AND :to
                         AND (b.categoryId IS NULL OR t.categoryId = b.categoryId)
                   ), 0) * 100.0 / b.limitAmount
               ELSE 0 END AS progressPercent
        FROM budgets b
        WHERE b.isActive = 1
        """
    )
    fun observeBudgetStatus(from: Long, to: Long): Flow<List<BudgetStatusRow>>

    // ── Multi-category join table ──────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(rows: List<com.myexpense.tracker.data.database.entity.BudgetCategoryEntity>)

    @Query("DELETE FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun deleteCategoriesFor(budgetId: Long)

    @Query("SELECT * FROM budget_categories")
    fun observeAllCategories(): Flow<List<com.myexpense.tracker.data.database.entity.BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId")
    fun observeCategories(budgetId: Long): Flow<List<com.myexpense.tracker.data.database.entity.BudgetCategoryEntity>>
}
