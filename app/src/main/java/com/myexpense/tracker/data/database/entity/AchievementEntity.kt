package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Gamification achievements. */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val type: String = "GENERAL",
)
