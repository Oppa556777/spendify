package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.MonthTotalRow
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.TransactionEntity
import com.myexpense.tracker.data.model.CategoryStat
import com.myexpense.tracker.data.model.DailyStat
import com.myexpense.tracker.data.model.MonthlyPoint
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.utils.endMillis
import com.myexpense.tracker.utils.startMillis
import com.myexpense.tracker.utils.toEpochMillis
import com.myexpense.tracker.utils.toLocalDate
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        dao.observeBetween(from.toEpochMillis(), to.toEpochMillis()).map { list -> list.map { it.toModel() } }

    fun observeFiltered(
        month: YearMonth?,
        type: TransactionType?,
        categoryId: Long?,
        accountId: Long?,
        query: String,
    ): Flow<List<Transaction>> =
        dao.observeFiltered(
            from = month?.startMillis(),
            to = month?.endMillis(),
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            query = query.trim(),
        ).map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): Transaction? = dao.getById(id)?.toModel()

    fun observeIncomeForMonth(month: YearMonth): Flow<Long> =
        dao.observeIncomeBetween(month.startMillis(), month.endMillis()).map { it.toMinorUnits() }

    fun observeExpenseForMonth(month: YearMonth): Flow<Long> =
        dao.observeExpenseBetween(month.startMillis(), month.endMillis()).map { it.toMinorUnits() }

    /** All-time income and expense (minor units). */
    fun observeTotals(): Flow<Pair<Long, Long>> =
        combine(dao.observeTotalIncome(), dao.observeTotalExpense()) { income, expense ->
            income.toMinorUnits() to expense.toMinorUnits()
        }

    /** Income and expense (minor units) between two dates, per account. */
    fun observePeriodTotals(from: Long, to: Long, accountId: Long?): Flow<Pair<Long, Long>> =
        combine(
            dao.observeIncomeBetween(from, to, accountId),
            dao.observeExpenseBetween(from, to, accountId),
        ) { income, expense -> income.toMinorUnits() to expense.toMinorUnits() }

    /** Last [days] days of income/expense for the overview chart. */
    fun observeDailySeries(days: Int = 7): Flow<List<DailyStat>> {
        val today = LocalDate.now()
        val from = today.minusDays((days - 1).toLong())
        return dao.observeDailyTotals(from.toEpochMillis(), today.toEpochMillis()).map { rows ->
            val byDay = rows.groupBy { it.day }
            (0 until days).map { offset ->
                val day = from.plusDays(offset.toLong())
                val key = day.toString()
                DailyStat(
                    date = day,
                    income = byDay[key]?.firstOrNull { it.type == TransactionType.INCOME }?.total?.toMinorUnits() ?: 0L,
                    expense = byDay[key]?.firstOrNull { it.type == TransactionType.EXPENSE }?.total?.toMinorUnits() ?: 0L,
                )
            }
        }
    }

    /** Per-category expense stats for a month ("yyyy-MM"). */
    fun observeCategoryStats(monthKey: String, type: TransactionType): Flow<List<CategoryStat>> {
        val month = runCatching { YearMonth.parse(monthKey) }.getOrNull()
            ?: return flowOf(emptyList())
        return dao.observeCategoryTotals(month.startMillis(), month.endMillis(), type).map { rows ->
            rows.map { row ->
                CategoryStat(
                    categoryId = row.categoryId,
                    total = row.total.toMinorUnits(),
                    count = row.count,
                )
            }
        }
    }

    /** Monthly income/expense series between two dates (inclusive). */
    fun observeMonthlySeries(from: LocalDate, to: LocalDate): Flow<List<MonthlyPoint>> =
        dao.observeMonthlyTotals(from.toEpochMillis(), to.toEpochMillis()).map { rows ->
            buildSeries(YearMonth.from(from), YearMonth.from(to), rows)
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

    private fun buildSeries(
        from: YearMonth,
        to: YearMonth,
        rows: List<MonthTotalRow>,
    ): List<MonthlyPoint> {
        val grouped = rows.groupBy { it.month }
        val points = mutableListOf<MonthlyPoint>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            val key = cursor.toString()
            points += MonthlyPoint(
                month = cursor,
                income = grouped[key]?.firstOrNull { it.type == TransactionType.INCOME }?.total?.toMinorUnits() ?: 0L,
                expense = grouped[key]?.firstOrNull { it.type == TransactionType.EXPENSE }?.total?.toMinorUnits() ?: 0L,
            )
            cursor = cursor.plusMonths(1)
        }
        return points
    }

    private fun TransactionEntity.toModel() = Transaction(
        id = id,
        title = title,
        amount = amount.toMinorUnits(),
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = toAccountId,
        note = note ?: "",
        date = date.toLocalDate(),
        time = time,
        tags = tags,
        personId = personId,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        receiptImagePath = receiptImagePath,
        isRecurring = isRecurring,
        recurringId = recurringId,
        createdAt = createdAt,
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        title = title,
        amount = amount.toRupees(),
        type = type,
        categoryId = categoryId,
        accountId = requireNotNull(accountId) { "Transaction requires an account" },
        toAccountId = toAccountId,
        note = note.ifBlank { null },
        date = date.toEpochMillis(),
        time = time,
        tags = tags,
        personId = personId,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        receiptImagePath = receiptImagePath,
        isRecurring = isRecurring,
        recurringId = recurringId,
        createdAt = createdAt,
    )
}

/** Defaults used by various view models. */
fun currentMonth(): YearMonth = YearMonth.now()
fun monthKey(yearMonth: YearMonth): String = yearMonth.toString()
