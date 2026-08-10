package com.myexpense.tracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myexpense.tracker.data.database.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

/** Key-value settings store. */
@Dao
interface AppSettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingEntity>

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun observeValue(key: String): Flow<String?>

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
