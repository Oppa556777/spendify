package com.myexpense.tracker.data.model

/** A tag used to classify transactions. */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long = 0xFF6C63FF,
    val createdAt: Long = System.currentTimeMillis(),
)
