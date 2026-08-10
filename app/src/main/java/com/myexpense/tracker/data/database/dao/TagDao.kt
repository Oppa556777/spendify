package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @androidx.room.Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
