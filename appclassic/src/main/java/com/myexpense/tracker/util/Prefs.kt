package com.myexpense.tracker.util

import android.content.Context

/** Settings stored in SharedPreferences (framework-only, no DataStore). */
object Prefs {

    private const val NAME = "moneymate_prefs"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    fun currency(context: Context): String =
        prefs(context).getString("currency", "$") ?: "$"

    fun setCurrency(context: Context, symbol: String) {
        prefs(context).edit().putString("currency", symbol).apply()
    }

    fun themeMode(context: Context): Int = prefs(context).getInt("theme", THEME_SYSTEM)

    fun setThemeMode(context: Context, mode: Int) {
        prefs(context).edit().putInt("theme", mode).apply()
    }

    fun biometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean("biometric", false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("biometric", enabled).apply()
    }

    fun firstRunDone(context: Context): Boolean =
        prefs(context).getBoolean("first_run", false)

    fun setFirstRunDone(context: Context) {
        prefs(context).edit().putBoolean("first_run", true).apply()
    }

    fun setPendingAction(context: Context, action: String) {
        prefs(context).edit().putString("pending_action", action).apply()
    }

    fun takePendingAction(context: Context): String? {
        val p = prefs(context)
        val a = p.getString("pending_action", null)
        if (a != null) p.edit().remove("pending_action").apply()
        return a
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
