package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<SubscriptionEntity>)

    @androidx.room.Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions ORDER BY nextDueDate ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun observeActive(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 AND nextDueDate BETWEEN :from AND :to ORDER BY nextDueDate ASC")
    fun observeDueBetween(from: Long, to: Long): Flow<List<SubscriptionEntity>>

    @Query("UPDATE subscriptions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}
