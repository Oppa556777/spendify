package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.PersonDao
import com.myexpense.tracker.data.database.entity.PersonEntity
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val dao: PersonDao,
) {

    fun observeAll(): Flow<List<Person>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Person> = dao.getAll().map { it.toModel() }

    suspend fun save(name: String, phone: String?, color: Long): Long =
        dao.insert(PersonEntity(name = name, phone = phone?.ifBlank { null }, avatarColor = color.toHexColor()))

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun PersonEntity.toModel() = Person(
        id = id,
        name = name,
        phone = phone,
        avatarColor = avatarColor.toColorLong(),
        createdAt = createdAt,
    )
}
