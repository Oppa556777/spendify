package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.BillSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillSplitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: BillSplitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<BillSplitEntity>)

    @androidx.room.Update
    suspend fun update(split: BillSplitEntity)

    @Query("DELETE FROM bill_splits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bill_splits WHERE id = :id")
    suspend fun getById(id: Long): BillSplitEntity?

    @Query("SELECT * FROM bill_splits ORDER BY date DESC")
    fun observeAll(): Flow<List<BillSplitEntity>>

    @Query("SELECT * FROM bill_splits ORDER BY date DESC")
    suspend fun getAll(): List<BillSplitEntity>

    @Query("DELETE FROM bill_splits")
    suspend fun deleteAll()
}
