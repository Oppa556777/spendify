package com.myexpense.tracker.data.model

/** Spending/income bucket used to classify transactions. */
data class Category(
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val icon: String = "category",          // material icon identifier
    val color: Long = 0xFF4CAF50,           // opaque ARGB
    val parentId: Long? = null,             // null = top-level, else = subcategory
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
