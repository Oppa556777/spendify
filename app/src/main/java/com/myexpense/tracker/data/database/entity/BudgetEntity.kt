package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.BudgetPeriod

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("isActive")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,           // null = all categories
    val limitAmount: Double,
    val spentAmount: Double = 0.0,          // cached/calculated
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long,
    val endDate: Long? = null,
    val colorHex: String = "#4CAF50",
    val alertAt: Int = 80,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
