package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.BudgetDao
import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.BudgetEntity
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.utils.endMillis
import com.myexpense.tracker.utils.startMillis
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
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

    fun observeAll(): Flow<List<Budget>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Active budgets with category info and the amount spent in [month]. */
    fun observeForMonthWithSpent(month: YearMonth): Flow<List<BudgetWithSpent>> =
        combine(
            dao.observeActive(),
            transactionDao.observeCategoryTotals(
                month.startMillis(),
                month.endMillis(),
                TransactionType.EXPENSE,
            ),
            categoryDao.observeAll(),
        ) { budgets, totals, categories ->
            val categoryMap = categories.associateBy { it.id }
            val spentByCategory = totals.associate { it.categoryId to it.total.toMinorUnits() }
            budgets.map { budget ->
                val spent = if (budget.categoryId == null) {
                    spentByCategory.values.sum()
                } else {
                    spentByCategory[budget.categoryId] ?: 0L
                }
                BudgetWithSpent(
                    budget = budget.toModel(),
                    category = budget.categoryId?.let { categoryMap[it] }?.toModel(),
                    spent = spent,
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
        name = name,
        categoryId = categoryId,
        limitAmount = limitAmount.toMinorUnits(),
        spentAmount = spentAmount.toMinorUnits(),
        period = period,
        startDate = startDate,
        endDate = endDate,
        colorHex = colorHex,
        alertAt = alertAt,
        isActive = isActive,
        createdAt = createdAt,
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        limitAmount = limitAmount.toRupees(),
        spentAmount = spentAmount.toRupees(),
        period = period,
        startDate = startDate,
        endDate = endDate,
        colorHex = colorHex,
        alertAt = alertAt,
        isActive = isActive,
        createdAt = createdAt,
    )

    private fun CategoryEntity.toModel() = Category(
        id = id,
        name = name,
        type = type,
        icon = iconName,
        color = colorHex.toColorLong(),
        parentId = parentId,
        isDefault = isDefault,
        createdAt = createdAt,
    )
}
