package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.TransactionType

/**
 * The central transactions table.
 *
 * - [date] is an epoch-millisecond timestamp (UTC midnight of the local day).
 * - [categoryId] / [accountId] are NOT NULL (any transaction must be placed
 *   somewhere); RESTRICT prevents silently losing financial history.
 * - [tags] holds comma-separated tag IDs.
 */
@Entity(
    tableName = "transactions",
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
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("date"),
        Index("categoryId"),
        Index("accountId"),
        Index("toAccountId"),
        Index("personId"),
        Index("type")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val note: String? = null,
    val date: Long,
    val time: String = "",
    val tags: String? = null,
    val personId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val receiptImagePath: String? = null,
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
