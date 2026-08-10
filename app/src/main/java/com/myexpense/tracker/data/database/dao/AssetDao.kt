package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>)

    @androidx.room.Update
    suspend fun update(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: Long): AssetEntity?

    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    suspend fun getAll(): List<AssetEntity>

    /** Current market value of the whole portfolio. */
    @Query("SELECT COALESCE(SUM(quantity * currentPrice), 0) FROM assets")
    fun observeTotalValue(): Flow<Double>

    /** Total unrealised gain/loss. */
    @Query("SELECT COALESCE(SUM((currentPrice - buyPrice) * quantity), 0) FROM assets")
    fun observeTotalGain(): Flow<Double>

    @Query("DELETE FROM assets")
    suspend fun deleteAll()
}
