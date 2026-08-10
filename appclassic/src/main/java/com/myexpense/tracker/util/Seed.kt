package com.myexpense.tracker.util

import android.content.Context
import com.myexpense.tracker.db.Account
import com.myexpense.tracker.db.AccountType
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.Category
import com.myexpense.tracker.db.TxType

/** Seeds default categories + a cash account on first launch. */
object Seed {

    fun seedIfNeeded(context: Context) {
        if (Prefs.firstRunDone(context)) return
        val db = AppDb.get(context)
        if (db.getAllCategories().isEmpty()) {
            defaultCategories().forEach { db.insertCategory(it) }
        }
        if (db.getAccounts().isEmpty()) {
            db.insertAccount(Account(name = "Cash", type = AccountType.CASH, icon = "💵", color = 0xFF4CAF50.toInt()))
        }
        Prefs.setFirstRunDone(context)
    }

    fun defaultCategories(): List<Category> {
        fun c(name: String, type: TxType, icon: String, color: Int, order: Int) =
            Category(name = name, type = type, icon = icon, color = color, sortOrder = order)
        return listOf(
            c("Food & Dining", TxType.EXPENSE, "🍔", 0xFFEF5350.toInt(), 1),
            c("Groceries", TxType.EXPENSE, "🛒", 0xFFFFA726.toInt(), 2),
            c("Transport", TxType.EXPENSE, "🚌", 0xFF42A5F5.toInt(), 3),
            c("Fuel", TxType.EXPENSE, "⛽", 0xFF26A69A.toInt(), 4),
            c("Shopping", TxType.EXPENSE, "🛍️", 0xFFAB47BC.toInt(), 5),
            c("Entertainment", TxType.EXPENSE, "🎬", 0xFFEC407A.toInt(), 6),
            c("Health", TxType.EXPENSE, "🏥", 0xFFEF5350.toInt(), 7),
            c("Housing", TxType.EXPENSE, "🏠", 0xFF8D6E63.toInt(), 8),
            c("Utilities", TxType.EXPENSE, "⚡", 0xFFFFCA28.toInt(), 9),
            c("Education", TxType.EXPENSE, "🎓", 0xFF5C6BC0.toInt(), 10),
            c("Travel", TxType.EXPENSE, "✈️", 0xFF29B6F6.toInt(), 11),
            c("Bills & Fees", TxType.EXPENSE, "🧾", 0xFF78909C.toInt(), 12),
            c("Other", TxType.EXPENSE, "📦", 0xFF9E9E9E.toInt(), 99),
            c("Salary", TxType.INCOME, "💰", 0xFF66BB6A.toInt(), 1),
            c("Business", TxType.INCOME, "💼", 0xFF26A69A.toInt(), 2),
            c("Freelance", TxType.INCOME, "💻", 0xFF42A5F5.toInt(), 3),
            c("Investments", TxType.INCOME, "📈", 0xFFAB47BC.toInt(), 4),
            c("Gifts", TxType.INCOME, "🎁", 0xFFEC407A.toInt(), 5),
            c("Other Income", TxType.INCOME, "➕", 0xFF9E9E9E.toInt(), 99)
        )
    }

    val emojiIcons = listOf(
        "🍔", "🛒", "🚌", "⛽", "🛍️", "🎬", "🏥", "🏠", "⚡", "🎓", "✈️", "🧾", "📦",
        "💰", "💼", "💻", "📈", "🎁", "➕", "☕", "🍕", "🍺", "🎮", "🎵", "📱", "📺",
        "💊", "🏋️", "🐶", "👶", "💍", "🚕", "🚗", "🚆", "✂️", "🔧", "🧹", "🌿", "📚",
        "🧸", "🎂", "💐", "📷", "⌚", "👔", "🧥", "💳", "🏦", "💵", "📉", "📊", "⭐", "❓"
    )

    val colorPalette = listOf(
        0xFFE53935.toInt(), 0xFFD81B60.toInt(), 0xFF8E24AA.toInt(), 0xFF5E35B1.toInt(),
        0xFF3949AB.toInt(), 0xFF1E88E5.toInt(), 0xFF039BE5.toInt(), 0xFF00ACC1.toInt(),
        0xFF00897B.toInt(), 0xFF43A047.toInt(), 0xFF7CB342.toInt(), 0xFFC0CA33.toInt(),
        0xFFFDD835.toInt(), 0xFFFFB300.toInt(), 0xFFFB8C00.toInt(), 0xFFF4511E.toInt(),
        0xFF6D4C41.toInt(), 0xFF757575.toInt(), 0xFF546E7A.toInt()
    )
}
