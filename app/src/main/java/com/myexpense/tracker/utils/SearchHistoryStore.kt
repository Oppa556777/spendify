package com.myexpense.tracker.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Stores recent search queries locally (SharedPreferences). */
@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    fun get(): List<String> = prefs.getString("queries", "")?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()

    fun add(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val updated = (listOf(q) + get().filter { it != q }).take(10)
        prefs.edit().putString("queries", updated.joinToString("\u0001")).apply()
    }

    fun clear() {
        prefs.edit().remove("queries").apply()
    }
}
