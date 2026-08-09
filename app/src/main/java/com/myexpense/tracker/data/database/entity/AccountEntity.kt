package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myexpense.tracker.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val color: Long,
    val icon: String,
    val isArchived: Boolean = false,
)
