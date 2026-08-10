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

    /** Titles from transaction history (for smart suggestions). */
    fun observeRecentTitles(): Flow<List<String>> = dao.recentTitles()

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
        return observeDailyRange(today.minusDays((days - 1).toLong()), today)
    }

    /** Daily income/expense totals between two dates (inclusive). */
    fun observeDailyRange(from: LocalDate, to: LocalDate): Flow<List<DailyStat>> =
        dao.observeDailyTotals(from.toEpochMillis(), to.toEpochMillis()).map { rows ->
            val byDay = rows.groupBy { it.day }
            val days = java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt() + 1
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

    /** Per-category totals (minor units) between dates, for a type. */
    fun observeCategoryStatsBetween(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
    ): Flow<List<CategoryStat>> =
        dao.observeCategoryTotals(from.toEpochMillis(), to.toEpochMillis(), type).map { rows ->
            rows.map { row ->
                CategoryStat(
                    categoryId = row.categoryId,
                    total = row.total.toMinorUnits(),
                    count = row.count,
                )
            }
        }

    /** Per-category expense stats for a month ("yyyy-MM"). */
    fun observeCategoryStats(monthKey: String, type: TransactionType): Flow<List<CategoryStat>> {
        val month = runCatching { YearMonth.parse(monthKey) }.getOrNull()
            ?: return flowOf(emptyList())
        return observeCategoryStatsBetween(month.atDay(1), month.atEndOfMonth(), type)
    }

    /** Monthly income/expense series between two dates (inclusive). */
    fun observeMonthlySeries(from: LocalDate, to: LocalDate): Flow<List<MonthlyPoint>> =
        dao.observeMonthlyTotals(from.toEpochMillis(), to.toEpochMillis()).map { rows ->
            buildSeries(YearMonth.from(from), YearMonth.from(to), rows)
        }

    /** Monthly × category aggregation (for the category-trend chart). */
    fun observeMonthlyCategoryTotals(
        from: LocalDate,
        to: LocalDate,
        type: TransactionType,
    ): Flow<List<com.myexpense.tracker.data.database.dao.MonthlyCategoryRow>> =
        dao.observeMonthlyCategoryTotals(from.toEpochMillis(), to.toEpochMillis(), type)

    /** 30-day balance history for one account, reconstructed from today's balance. */
    fun observeAccountBalanceSeries(
        accountId: Long,
        currentBalanceMinor: Long,
        days: Int = 30,
    ): Flow<List<Pair<LocalDate, Long>>> {
        val today = LocalDate.now()
        val from = today.minusDays((days - 1).toLong())
        return dao.observeAccountDailyNet(accountId, from.toEpochMillis(), today.toEpochMillis()).map { rows ->
            val netByDay = rows.associate { it.day to it.net.toMinorUnits() }
            val result = mutableListOf<Pair<LocalDate, Long>>()
            var running = currentBalanceMinor
            // walk newest → oldest accumulating net, then reverse
            val backwards = (days - 1 downTo 0).map { offset ->
                val day = from.plusDays(offset.toLong())
                val value = running
                running -= (netByDay[day.toString()] ?: 0L)
                day to value
            }
            backwards.reversed().forEach { result.add(it) }
            result
        }
    }

    /** All transactions touching an account (source or destination). */
    fun observeForAccount(accountId: Long, type: TransactionType?): Flow<List<Transaction>> =
        dao.observeAccountTransactions(accountId, type).map { list -> list.map { it.toModel() } }

    /** The most recent transaction per account. */
    fun observeLatestPerAccount(): Flow<List<Transaction>> =
        dao.observeLatestPerAccount().map { list -> list.map { it.toModel() } }

    /** Advanced search with optional filters and Kotlin-side sorting. */
    fun searchAdvanced(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        categoryId: Long?,
        accountId: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        tagId: Long?,
        personId: Long?,
    ): Flow<List<Transaction>> =
        dao.searchAdvanced(
            query = query.trim(),
            from = from?.toEpochMillis(),
            to = to?.toEpochMillis(),
            categoryId = categoryId,
            accountId = accountId,
            minAmount = minAmount,
            maxAmount = maxAmount,
            tagId = tagId,
            personId = personId,
        ).map { list -> list.map { it.toModel() } }

    suspend fun countByTag(tagId: Long): Int = dao.countByTag(tagId)

    suspend fun countByPerson(personId: Long): Int = dao.countByPerson(personId)

    suspend fun personNet(personId: Long): Long = dao.personNet(personId).toMinorUnits()

    fun observeByPerson(personId: Long): Flow<List<Transaction>> =
        dao.observeByPerson(personId).map { list -> list.map { it.toModel() } }

    /** Expense transactions in any of the given categories between dates. */
    fun observeExpensesForCategories(
        categoryIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Transaction>> =
        if (categoryIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else dao.observeByCategories(categoryIds, from.toEpochMillis(), to.toEpochMillis())
            .map { list -> list.map { it.toModel() } }

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
