package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.LoanType

@Entity(
    tableName = "loans",
    indices = [Index("isSettled"), Index("type")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LoanType,                     // LENT / BORROWED
    val personName: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: Long? = null,
    val note: String? = null,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
