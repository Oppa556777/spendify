package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A shared bill (e.g. dinner with friends) split between people. */
@Entity(tableName = "bill_splits")
data class BillSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val date: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
