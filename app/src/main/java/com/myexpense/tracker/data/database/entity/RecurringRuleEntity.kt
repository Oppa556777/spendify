package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.RecurringFrequency
import com.myexpense.tracker.data.model.TransactionType

/**
 * Templates for automatic recurring transactions. The app is expected to
 * generate transactions from these rules (e.g. on app start).
 */
@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("isActive")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long,
    val frequency: RecurringFrequency,
    val interval: Int = 1,                  // every N days/weeks/months/years
    val startDate: Long,
    val endDate: Long? = null,
    val lastExecuted: Long? = null,
    val isActive: Boolean = true,
)
