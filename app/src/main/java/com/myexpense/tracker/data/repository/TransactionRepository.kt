package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.TransactionEntity
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) {

    fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeRecent(limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    fun observeFiltered(
        month: YearMonth?,
        type: TransactionType?,
        categoryId: Long?,
        accountId: Long?,
        query: String,
    ): Flow<List<Transaction>> =
        dao.observeFiltered(
            month = month?.toString(),
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            query = query.trim(),
        ).map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toModel()

    fun observeIncomeForMonth(month: YearMonth): Flow<Long> =
        dao.observeIncomeForMonth(month.toString())

    fun observeExpenseForMonth(month: YearMonth): Flow<Long> =
        dao.observeExpenseForMonth(month.toString())

    fun observeTotals(): Flow<Pair<Long, Long>> {
        // income, expense (all time)
        return kotlinx.coroutines.flow.combine(
            dao.observeTotalIncome(),
            dao.observeTotalExpense(),
        ) { income, expense -> income to expense }
    }

    suspend fun save(transaction: Transaction): Long {
        val entity = transaction.toEntity()
        return if (transaction.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            transaction.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun insertAll(transactions: List<Transaction>) =
        dao.insertAll(transactions.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun TransactionEntity.toModel() = Transaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        accountId = accountId,
        note = note,
        date = date,
        createdAt = createdAt,
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        accountId = accountId,
        note = note,
        date = date,
        createdAt = createdAt,
    )
}

/** Defaults used by various view models. */
fun currentMonth(): YearMonth = YearMonth.now()
fun monthKey(yearMonth: YearMonth): String = yearMonth.toString()
