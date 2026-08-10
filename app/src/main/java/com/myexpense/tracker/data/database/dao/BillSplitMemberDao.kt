package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.BillSplitMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillSplitMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: BillSplitMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<BillSplitMemberEntity>)

    @androidx.room.Update
    suspend fun update(member: BillSplitMemberEntity)

    @Query("DELETE FROM bill_split_members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bill_split_members WHERE id = :id")
    suspend fun getById(id: Long): BillSplitMemberEntity?

    @Query("SELECT * FROM bill_split_members WHERE splitId = :splitId ORDER BY id ASC")
    fun observeBySplit(splitId: Long): Flow<List<BillSplitMemberEntity>>

    @Query("SELECT * FROM bill_split_members ORDER BY splitId ASC, id ASC")
    fun observeAll(): Flow<List<BillSplitMemberEntity>>

    @Query("SELECT * FROM bill_split_members WHERE splitId = :splitId ORDER BY id ASC")
    suspend fun getBySplit(splitId: Long): List<BillSplitMemberEntity>

    @Query("SELECT COALESCE(SUM(shareAmount), 0) FROM bill_split_members WHERE splitId = :splitId")
    fun observeTotalShare(splitId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(shareAmount), 0) FROM bill_split_members WHERE splitId = :splitId AND isPaid = 1")
    fun observePaidShare(splitId: Long): Flow<Double>

    @Query("UPDATE bill_split_members SET isPaid = :isPaid WHERE id = :id")
    suspend fun setPaid(id: Long, isPaid: Boolean)

    @Query("DELETE FROM bill_split_members WHERE splitId = :splitId")
    suspend fun deleteBySplit(splitId: Long)

    @Query("DELETE FROM bill_split_members")
    suspend fun deleteAll()
}
