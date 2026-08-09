package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.CategoryDao
import com.myexpense.tracker.data.database.entity.CategoryEntity
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
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

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun insertAll(categories: List<Category>) =
        dao.insertAll(categories.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun CategoryEntity.toModel() = Category(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isDefault = isDefault,
        sortOrder = sortOrder,
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isDefault = isDefault,
        sortOrder = sortOrder,
    )
}
