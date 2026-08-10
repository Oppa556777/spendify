package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.BillingCycle

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("nextDueDate"), Index("isActive")]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val billingCycle: BillingCycle,
    val nextDueDate: Long,
    val categoryId: Long,
    val colorHex: String,
    val iconName: String,
    val reminderDays: Int = 3,
    val isActive: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
