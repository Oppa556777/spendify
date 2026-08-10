package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One person's share of a bill split. Deleting a split cascades to its members.
 */
@Entity(
    tableName = "bill_split_members",
    foreignKeys = [
        ForeignKey(
            entity = BillSplitEntity::class,
            parentColumns = ["id"],
            childColumns = ["splitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("splitId"), Index("personId")]
)
data class BillSplitMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitId: Long,
    val personId: Long,
    val shareAmount: Double,
    val isPaid: Boolean = false,
)
