package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [Index("type")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val icon: String,
    val color: Long,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)
