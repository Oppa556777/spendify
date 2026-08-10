package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/** Goal progress percentage computed in SQL. */
data class GoalProgressRow(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val progressPercent: Double,
)

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @androidx.room.Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY deadline ASC, createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isCompleted = 0")
    suspend fun getActive(): List<GoalEntity>

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    suspend fun getAll(): List<GoalEntity>

    /** Goal progress percentage for every goal. */
    @Query(
        """
        SELECT id, name, targetAmount, savedAmount,
               CASE WHEN targetAmount > 0 THEN savedAmount * 100.0 / targetAmount ELSE 0 END AS progressPercent
        FROM goals
        ORDER BY createdAt DESC
        """
    )
    fun observeProgress(): Flow<List<GoalProgressRow>>

    @Query("UPDATE goals SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
