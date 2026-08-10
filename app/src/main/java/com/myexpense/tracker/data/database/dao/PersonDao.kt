package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)

    @androidx.room.Update
    suspend fun update(person: PersonEntity)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM people ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getAll(): List<PersonEntity>

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
