package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.TransactionEntity
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Row returned by the monthly-series aggregation. */
data class MonthSumRow(
    val month: String,      // "2025-01"
    val type: TransactionType,
    val total: Long,
)

/** Aggregated amount + count per category. */
data class CategorySumRow(
    val categoryId: Long?,
    val total: Long,
    val count: Int,
)

/** Account id → net balance contribution. */
data class AccountSumRow(
    val accountId: Long,
    val total: Long,
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @androidx.room.Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :from AND date <= :to ORDER BY date DESC, createdAt DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /**
     * Filtered view used by the transactions list & search screen.
     * `month` filters by the exact ISO month prefix; pass null to ignore.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:month IS NULL OR date LIKE :month || '%')
          AND (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:query = '' OR note LIKE '%' || :query || '%'
               OR categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%'))
        ORDER BY date DESC, createdAt DESC
        """
    )
    fun observeFiltered(
        month: String?,
        type: TransactionType?,
        categoryId: Long?,
        accountId: Long?,
        query: String,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) FROM transactions WHERE date LIKE :month || '%'")
    fun observeIncomeForMonth(month: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) FROM transactions WHERE date LIKE :month || '%'")
    fun observeExpenseForMonth(month: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) FROM transactions")
    fun observeTotalIncome(): Flow<Long>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) FROM transactions")
    fun observeTotalExpense(): Flow<Long>

    @Query(
        """
        SELECT categoryId, SUM(amount) as total, COUNT(*) as count
        FROM transactions
        WHERE type = :type AND date LIKE :month || '%'
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeCategoryStats(month: String, type: TransactionType): Flow<List<CategorySumRow>>

    @Query(
        """
        SELECT strftime('%Y-%m', date) as month, type, SUM(amount) as total
        FROM transactions
        WHERE date >= :from AND date <= :to
        GROUP BY strftime('%Y-%m', date), type
        ORDER BY month ASC
        """
    )
    fun observeMonthlySeries(from: LocalDate, to: LocalDate): Flow<List<MonthSumRow>>

    @Query(
        """
        SELECT accountId, SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) as total
        FROM transactions
        WHERE accountId IS NOT NULL
        GROUP BY accountId
        """
    )
    fun observeBalanceByAccount(): Flow<List<AccountSumRow>>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0)
        FROM transactions
        WHERE accountId = :accountId
        """
    )
    fun observeAccountDelta(accountId: Long): Flow<Long>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
