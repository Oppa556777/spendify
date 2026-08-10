package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.TransactionEntity
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/** Row of the monthly income/expense aggregation (month = "yyyy-MM"). */
data class MonthTotalRow(
    val month: String,
    val type: TransactionType,
    val total: Double,
)

/** Row of the per-category aggregation. */
data class CategoryTotalRow(
    val categoryId: Long?,
    val total: Double,
    val count: Int,
)

/** One day of the daily income/expense aggregation (day = "yyyy-MM-dd"). */
data class DailyTotalRow(
    val day: String,
    val type: TransactionType,
    val total: Double,
)

/** One month × category cell of the aggregation. */
data class MonthlyCategoryRow(
    val month: String,          // "yyyy-MM"
    val categoryId: Long?,
    val total: Double,
)

/** Income vs expense for a period in one row. */
data class PeriodTotalsRow(
    val income: Double,
    val expense: Double,
)

@Dao
interface TransactionDao {

    // ── CRUD ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @androidx.room.Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /** Distinct titles from history — used for smart suggestions in the add form. */
    @Query("SELECT DISTINCT title FROM transactions WHERE title != '' ORDER BY id DESC LIMIT 30")
    fun recentTitles(): Flow<List<String>>

    // ── Date-range filters & search ─────────────────────────────────────────
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    /**
     * Filtered view used by the transactions list & search screen.
     * All filters are optional; `query` searches title, note and category name.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:from IS NULL OR date >= :from)
          AND (:to IS NULL OR date <= :to)
          AND (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:query = '' OR title LIKE '%' || :query || '%'
               OR note LIKE '%' || :query || '%'
               OR categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%'))
        ORDER BY date DESC, id DESC
        """
    )
    fun observeFiltered(
        from: Long?,
        to: Long?,
        type: TransactionType?,
        categoryId: Long?,
        accountId: Long?,
        query: String,
    ): Flow<List<TransactionEntity>>

    // ── Aggregations ────────────────────────────────────────────────────────
    /** Monthly income/expense totals for a date range, e.g. "2026-08". */
    @Query(
        """
        SELECT strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS month,
               type,
               SUM(amount) AS total
        FROM transactions
        WHERE date BETWEEN :from AND :to
        GROUP BY month, type
        ORDER BY month ASC
        """
    )
    fun observeMonthlyTotals(from: Long, to: Long): Flow<List<MonthTotalRow>>

    /** Per-category totals for a period and type (largest first). */
    @Query(
        """
        SELECT categoryId, SUM(amount) AS total, COUNT(*) AS count
        FROM transactions
        WHERE type = :type AND date BETWEEN :from AND :to
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(from: Long, to: Long, type: TransactionType): Flow<List<CategoryTotalRow>>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) " +
            "FROM transactions WHERE date BETWEEN :from AND :to"
    )
    fun observeIncomeBetween(from: Long, to: Long): Flow<Double>

    /** Income between dates, optionally restricted to one account. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) " +
            "FROM transactions WHERE date BETWEEN :from AND :to AND (:accountId IS NULL OR accountId = :accountId)"
    )
    fun observeIncomeBetween(from: Long, to: Long, accountId: Long?): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) " +
            "FROM transactions WHERE date BETWEEN :from AND :to"
    )
    fun observeExpenseBetween(from: Long, to: Long): Flow<Double>

    /** Expense between dates, optionally restricted to one account. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) " +
            "FROM transactions WHERE date BETWEEN :from AND :to AND (:accountId IS NULL OR accountId = :accountId)"
    )
    fun observeExpenseBetween(from: Long, to: Long, accountId: Long?): Flow<Double>

    /** Daily income/expense totals for the 7-day overview chart. */
    @Query(
        """
        SELECT strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS day,
               type,
               SUM(amount) AS total
        FROM transactions
        WHERE date BETWEEN :from AND :to
        GROUP BY day, type
        ORDER BY day ASC
        """
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DailyTotalRow>>

    /** Monthly × category aggregation for the category-trend chart. */
    @Query(
        """
        SELECT strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS month,
               categoryId,
               SUM(amount) AS total
        FROM transactions
        WHERE type = :type AND date BETWEEN :from AND :to
        GROUP BY month, categoryId
        ORDER BY month ASC
        """
    )
    fun observeMonthlyCategoryTotals(from: Long, to: Long, type: TransactionType): Flow<List<MonthlyCategoryRow>>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) FROM transactions")
    fun observeTotalIncome(): Flow<Double>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) FROM transactions")
    fun observeTotalExpense(): Flow<Double>

    /** Total income vs expense for a period in a single row. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        WHERE date BETWEEN :from AND :to
        """
    )
    fun observePeriodTotals(from: Long, to: Long): Flow<PeriodTotalsRow>

    // ── Maintenance used by safe-delete flows ───────────────────────────────
    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("UPDATE transactions SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun reassignAccount(oldAccountId: Long, newAccountId: Long)
}
