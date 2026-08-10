package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @androidx.room.Update
    suspend fun update(achievement: AchievementEntity)

    @Query("DELETE FROM achievements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getById(id: Long): AchievementEntity?

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun observeUnlocked(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 0 ORDER BY id ASC")
    fun observeLocked(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    suspend fun getAll(): List<AchievementEntity>

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :at WHERE id = :id")
    suspend fun markUnlocked(id: Long, at: Long)

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
