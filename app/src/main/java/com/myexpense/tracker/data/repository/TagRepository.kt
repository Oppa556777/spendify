package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.TagDao
import com.myexpense.tracker.data.database.entity.TagEntity
import com.myexpense.tracker.data.model.Tag
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val dao: TagDao,
) {

    fun observeAll(): Flow<List<Tag>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Tag> = dao.getAll().map { it.toModel() }

    suspend fun save(name: String, color: Long): Long =
        dao.insert(TagEntity(name = name, colorHex = color.toHexColor()))

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun TagEntity.toModel() = Tag(
        id = id,
        name = name,
        color = colorHex.toColorLong(),
        createdAt = createdAt,
    )
}
