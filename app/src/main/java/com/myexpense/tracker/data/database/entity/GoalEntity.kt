package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("accountId"), Index("isCompleted")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val iconName: String,
    val colorHex: String,
    val accountId: Long? = null,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
