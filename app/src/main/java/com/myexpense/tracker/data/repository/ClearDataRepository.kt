package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/** Wipes all on-device data (Room tables + DataStore settings). */
@Singleton
class ClearDataRepository @Inject constructor(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun clearAll() {
        database.clearAllTables()
        settingsRepository.resetAll()
    }
}
