package com.myexpense.tracker.data.model

/** A contact that can be associated with transactions, loans or splits. */
data class Person(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val avatarColor: Long = 0xFF3B82F6,
    val createdAt: Long = System.currentTimeMillis(),
)
