package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @androidx.room.Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules ORDER BY startDate ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 ORDER BY startDate ASC")
    fun observeActive(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    suspend fun getActive(): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("UPDATE recurring_rules SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun reassignAccount(oldAccountId: Long, newAccountId: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}
