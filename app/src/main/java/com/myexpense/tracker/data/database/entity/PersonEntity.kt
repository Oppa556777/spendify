package com.myexpense.tracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val avatarColor: String,
    val createdAt: Long = System.currentTimeMillis(),
)
