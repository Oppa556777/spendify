package com.myexpense.tracker.data.model

/** Spending/income bucket used to classify transactions. */
data class Category(
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val icon: String = "category",
    val color: Long = 0xFF4CAF50,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)
