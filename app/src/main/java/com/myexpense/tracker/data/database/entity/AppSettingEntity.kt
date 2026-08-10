package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Simple key-value store for app settings. */
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
