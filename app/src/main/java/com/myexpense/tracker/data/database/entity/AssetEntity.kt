package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.AssetType

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AssetType,
    val quantity: Double,
    val buyPrice: Double,
    val currentPrice: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
