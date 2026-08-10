package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.BudgetCategoryEntity
import com.myexpense.tracker.data.database.entity.BudgetEntity
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetPeriod
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.utils.endMillis
import com.myexpense.tracker.utils.startMillis
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toEpochMillis
import com.myexpense.tracker.utils.toHexColor
import com.myexpense.tracker.utils.toLocalDate
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    // ── Window helpers ─────────────────────────────────────────────────────

    /** The full period window for a budget (for its own spent calculation). */
    fun budgetWindow(budget: Budget, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        return when (budget.period) {
            BudgetPeriod.DAILY -> today to today
            BudgetPeriod.WEEKLY -> {
                val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                monday to monday.plusDays(6)
            }
            BudgetPeriod.MONTHLY -> {
                val ym = YearMonth.from(today)
                ym.atDay(1) to ym.atEndOfMonth()
            }
            BudgetPeriod.YEARLY -> {
                LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
            }
            BudgetPeriod.CUSTOM -> {
                val start = budget.startDate.toLocalDate()
                val end = budget.endDate?.toLocalDate() ?: today
                minOf(start, end) to maxOf(start, end)
            }
        }
    }

    private fun categoryIdsFor(entity: BudgetEntity, joins: List<BudgetCategoryEntity>): List<Long> =
        if (entity.categoryId == null) emptyList()
        else joins.filter { it.budgetId == entity.id }.map { it.categoryId }.ifEmpty { listOf(entity.categoryId) }

    private fun toModel(entity: BudgetEntity, joins: List<BudgetCategoryEntity>): Budget = Budget(
        id = entity.id,
        name = entity.name,
        categoryId = entity.categoryId,
        categoryIds = categoryIdsFor(entity, joins),
        limitAmount = entity.limitAmount.toMinorUnits(),
        spentAmount = entity.spentAmount.toMinorUnits(),
        period = entity.period,
        startDate = entity.startDate,
        endDate = entity.endDate,
        colorHex = entity.colorHex,
        alertAt = entity.alertAt,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
    )

    private fun toEntity(budget: Budget): BudgetEntity = BudgetEntity(
        id = budget.id,
        name = budget.name,
        categoryId = budget.categoryId,
        limitAmount = budget.limitAmount.toRupees(),
        spentAmount = budget.spentAmount.toRupees(),
        period = budget.period,
        startDate = budget.startDate,
        endDate = budget.endDate,
        colorHex = budget.colorHex,
        alertAt = budget.alertAt,
        isActive = budget.isActive,
        createdAt = budget.createdAt,
    )

    private fun CategoryEntity.toModel(): Category = Category(
        id = id,
        name = name,
        type = type,
        icon = iconName,
        color = colorHex.toColorLong(),
        parentId = parentId,
        isDefault = isDefault,
        createdAt = createdAt,
    )

    // ── Core: all budgets with spent within their own windows ──────────────

    private data class BudgetsData(
        val budgets: List<BudgetEntity>,
        val joins: List<BudgetCategoryEntity>,
        val categories: List<CategoryEntity>,
    )

    fun observeBudgetsWithSpent(): Flow<List<BudgetWithSpent>> =
        combine(dao.observeAll(), dao.observeAllCategories(), categoryDao.observeAll()) { b, j, c ->
            BudgetsData(b, j, c)
        }.flatMapLatest { data ->
            val today = LocalDate.now()
            val from = data.budgets.map { budgetWindow(it.toModel(data.joins), today).first }
                .minOrNull() ?: today.minusMonths(13)
            val to = today
            transactionDao.observeDailyCategoryTotals(from.toEpochMillis(), to.toEpochMillis()).map { rows ->
                val catMap = data.categories.associateBy { it.id }
                val dayMap = rows.groupBy { it.day }.mapValues { (_, list) ->
                    list.associate { it.categoryId to it.total.toMinorUnits() }
                }
                data.budgets.map { entity ->
                    val model = entity.toModel(data.joins)
                    val (wf, wt) = budgetWindow(model, today)
                    val ids = if (model.categoryId == null) null else model.categoryIds
                    val spent = sumDayMap(dayMap, wf, wt, ids)
                    BudgetWithSpent(
                        budget = model,
                        category = model.categoryId?.let { catMap[it] }?.toModel(),
                        spent = spent,
                    )
                }
            }
        }

    private fun sumDayMap(
        dayMap: Map<String, Map<Long?, Long>>,
        from: LocalDate,
        to: LocalDate,
        ids: List<Long>?,
    ): Long {
        val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
        var total = 0L
        for (offset in 0 until days) {
            val key = from.plusDays(offset.toLong()).toString()
            val row = dayMap[key] ?: continue
            total += if (ids == null) {
                row.values.sum()
            } else {
                row.entries.sumOf { (catId, amount) ->
                    if (catId != null && catId in ids) amount else 0L
                }
            }
        }
        return total
    }

    // ── Single budget + daily series (for the detail screen) ───────────────

    fun observeBudgetWithSpent(id: Long): Flow<BudgetWithSpent?> =
        observeBudgetsWithSpent().map { list -> list.firstOrNull { it.budget.id == id } }

    /** Day-by-day spent within the budget's own window (empty days included). */
    fun observeBudgetDailySeries(id: Long): Flow<List<Pair<LocalDate, Long>>> =
        combine(dao.observeAll(), dao.observeAllCategories()) { budgets, joins ->
            budgets.firstOrNull { it.id == id }?.let { it to joins } ?: (null to joins)
        }.flatMapLatest { (entity, joins) ->
            if (entity == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val model = entity.toModel(joins)
                val (wf, wt) = budgetWindow(model)
                val ids = if (model.categoryId == null) null else model.categoryIds
                transactionDao.observeDailyCategoryTotals(wf.toEpochMillis(), wt.toEpochMillis()).map { rows ->
                    val dayMap = rows.groupBy { it.day }.mapValues { (_, list) ->
                        list.associate { it.categoryId to it.total.toMinorUnits() }
                    }
                    val days = ChronoUnit.DAYS.between(wf, wt).toInt() + 1
                    (0 until days).map { offset ->
                        val day = wf.plusDays(offset.toLong())
                        val row = dayMap[day.toString()] ?: emptyMap()
                        val spent = if (ids == null) {
                            row.values.sum()
                        } else {
                            row.entries.sumOf { (catId, amount) ->
                                if (catId != null && catId in ids) amount else 0L
                            }
                        }
                        day to spent
                    }
                }
            }
        }

    // ── Month-scoped list (kept for the home dashboard) ────────────────────

    fun observeForMonthWithSpent(month: YearMonth): Flow<List<BudgetWithSpent>> =
        observeBudgetsWithSpent()

    // ── CRUD ────────────────────────────────────────────────────────────────

    suspend fun getById(id: Long): Budget? = dao.getById(id)?.let {
        it.toModel(dao.observeAllCategories().first())
    }

    suspend fun save(budget: Budget): Long {
        val entity = toEntity(budget)
        val id = if (budget.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            budget.id
        }
        dao.deleteCategoriesFor(id)
        if (budget.categoryId != null && budget.categoryIds.isNotEmpty()) {
            dao.insertCategories(budget.categoryIds.distinct().map { BudgetCategoryEntity(id, it) })
        }
        return id
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun markCompleted(id: Long) {
        dao.getById(id)?.let { dao.update(it.copy(isActive = false)) }
    }

    suspend fun markActive(id: Long) {
        dao.getById(id)?.let { dao.update(it.copy(isActive = true)) }
    }

    suspend fun insertAll(budgets: List<Budget>) {
        dao.insertAll(budgets.map { toEntity(it) })
        budgets.forEach { b ->
            if (b.categoryId != null && b.categoryIds.isNotEmpty()) {
                dao.insertCategories(b.categoryIds.distinct().map { BudgetCategoryEntity(b.id, it) })
            }
        }
    }

    suspend fun getAll(): List<Budget> {
        val joins = dao.observeAllCategories().first()
        return dao.getAll().map { it.toModel(joins) }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
