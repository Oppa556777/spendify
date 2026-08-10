package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.dao.RecurringRuleDao
import com.myexpense.tracker.data.database.dao.SubscriptionDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val subscriptionDao: SubscriptionDao,
    private val recurringRuleDao: RecurringRuleDao,
) {

    fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeByType(type: TransactionType): Flow<List<Category>> =
        dao.observeByType(type).map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Category> = dao.getAll().map { it.toModel() }

    suspend fun getByType(type: TransactionType): List<Category> =
        dao.getByType(type).map { it.toModel() }

    suspend fun getById(id: Long): Category? = dao.getById(id)?.toModel()

    suspend fun save(category: Category): Long {
        val entity = category.toEntity()
        return if (category.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            category.id
        }
    }

    /**
     * Safe delete: transactions, subscriptions and recurring rules that point
     * at this category are reassigned to a fallback category of the same type.
     * Returns false when no fallback exists (nothing was deleted).
     */
    suspend fun delete(id: Long): Boolean {
        val category = dao.getById(id) ?: return false
        val fallback = dao.getByType(category.type).firstOrNull { it.id != id } ?: return false
        transactionDao.reassignCategory(id, fallback.id)
        subscriptionDao.reassignCategory(id, fallback.id)
        recurringRuleDao.reassignCategory(id, fallback.id)
        dao.deleteById(id)
        return true
    }

    suspend fun insertAll(categories: List<Category>) =
        dao.insertAll(categories.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

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

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        type = type,
        iconName = icon,
        colorHex = color.toHexColor(),
        parentId = parentId,
        isDefault = isDefault,
        createdAt = createdAt,
    )
}
