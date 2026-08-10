package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)

    @androidx.room.Update
    suspend fun update(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isSettled = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun observeActive(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isSettled = 0")
    suspend fun getActive(): List<LoanEntity>

    /** Outstanding amount (amount − paid) for money we lent out. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'LENT' THEN amount - paidAmount ELSE 0 END), 0) " +
            "FROM loans WHERE isSettled = 0"
    )
    fun observeOutstandingLent(): Flow<Double>

    /** Outstanding amount (amount − paid) for money we borrowed. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'BORROWED' THEN amount - paidAmount ELSE 0 END), 0) " +
            "FROM loans WHERE isSettled = 0"
    )
    fun observeOutstandingBorrowed(): Flow<Double>

    @Query("UPDATE loans SET isSettled = 1 WHERE id = :id")
    suspend fun markSettled(id: Long)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}
