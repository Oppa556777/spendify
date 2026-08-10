package com.myexpense.tracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myexpense.tracker.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Settings(
        val currencySymbol: String = "$",
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val dynamicColors: Boolean = true,
        val biometricEnabled: Boolean = false,
        val firstRunComplete: Boolean = false,
        val onboardingSeen: Boolean = false,
    )

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val THEME = intPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_colors")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val FIRST_RUN = booleanPreferencesKey("first_run_complete")
        val ONBOARDING = booleanPreferencesKey("onboarding_seen")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            currencySymbol = prefs[Keys.CURRENCY] ?: "$",
            themeMode = ThemeMode.fromOrdinal(prefs[Keys.THEME] ?: 0),
            dynamicColors = prefs[Keys.DYNAMIC] ?: true,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            firstRunComplete = prefs[Keys.FIRST_RUN] ?: false,
            // Existing users (seeded before onboarding existed) skip the flow.
            onboardingSeen = prefs[Keys.ONBOARDING] ?: (prefs[Keys.FIRST_RUN] ?: false),
        )
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = symbol }
    }

    /** Blocking-safe current currency symbol (usable inside runBlocking). */
    suspend fun currentSymbol(): String = settings.first().currencySymbol

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.ordinal }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    /** Marks first-launch seeding (default categories/achievements) as done. */
    suspend fun setSeedingDone() {
        context.dataStore.edit { it[Keys.FIRST_RUN] = true }
    }

    /** Marks onboarding (incl. the setup sheet) as complete — never shown again. */
    suspend fun setOnboardingComplete() {
        context.dataStore.edit {
            it[Keys.FIRST_RUN] = true
            it[Keys.ONBOARDING] = true
        }
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromOrdinal(value: Int): ThemeMode = entries.getOrElse(value) { SYSTEM }
    }
}
