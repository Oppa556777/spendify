package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.BudgetEntity
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetWithSpent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    fun observeForMonth(month: YearMonth): Flow<List<Budget>> =
        dao.observeForMonth(month).map { list -> list.map { it.toModel() } }

    /** Budgets with category info and how much was spent in [month]. */
    fun observeForMonthWithSpent(month: YearMonth): Flow<List<BudgetWithSpent>> =
        combine(
            dao.observeForMonth(month),
            categoryDao.observeAll(),
            transactionDao.observeCategoryStats(month.toString(), com.myexpense.tracker.data.model.TransactionType.EXPENSE),
        ) { budgets, categories, stats ->
            val categoryMap = categories.associateBy { it.id }
            val spentMap = stats.associate { it.categoryId to it.total }
            budgets.map { budget ->
                val category = budget.categoryId?.let(categoryMap::get)?.toModel()
                BudgetWithSpent(
                    budget = budget.toModel(),
                    category = category,
                    spent = spentMap[budget.categoryId] ?: 0L,
                    income = 0L,
                )
            }
        }

    suspend fun getById(id: Long): Budget? = dao.getById(id)?.toModel()

    suspend fun save(budget: Budget): Long {
        val entity = budget.toEntity()
        return if (budget.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            budget.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun insertAll(budgets: List<Budget>) =
        dao.insertAll(budgets.map { it.toEntity() })

    suspend fun getAll(): List<Budget> = dao.getAll().map { it.toModel() }

    suspend fun deleteAll() = dao.deleteAll()

    private fun BudgetEntity.toModel() = Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        isRecurring = isRecurring,
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        isRecurring = isRecurring,
    )

    private fun com.myexpense.tracker.data.database.entity.CategoryEntity.toModel() =
        com.myexpense.tracker.data.model.Category(
            id = id,
            name = name,
            type = type,
            icon = icon,
            color = color,
            isDefault = isDefault,
            sortOrder = sortOrder,
        )
}
