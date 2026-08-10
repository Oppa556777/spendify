package com.myexpense.tracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myexpense.tracker.data.model.Accent
import com.myexpense.tracker.data.model.DateFormat
import com.myexpense.tracker.data.model.FontSize
import com.myexpense.tracker.data.model.LockDelay
import com.myexpense.tracker.data.model.NumberFormat
import com.myexpense.tracker.data.model.ThemeMode
import com.myexpense.tracker.data.model.WeekStart
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Settings(
        val currencySymbol: String = "₹",
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val dynamicColors: Boolean = true,
        val accent: Accent = Accent.PURPLE,
        val fontSize: FontSize = FontSize.MEDIUM,
        val dateFormat: DateFormat = DateFormat.DDMMYYYY,
        val weekStart: WeekStart = WeekStart.MONDAY,
        val monthStartDay: Int = 1,
        val numberFormat: NumberFormat = NumberFormat.INDIAN,
        val biometricEnabled: Boolean = false,
        val lockDelay: LockDelay = LockDelay.IMMEDIATELY,
        val hideBalanceDefault: Boolean = false,
        val notifyBudget: Boolean = true,
        val notifyBill: Boolean = true,
        val notifySubscriptions: Boolean = true,
        val notifyLoans: Boolean = true,
        val notifyDaily: Boolean = false,
        val dailyReminderTime: String = "21:00",
        val notifyAchievements: Boolean = true,
        val firstRunComplete: Boolean = false,
        val onboardingSeen: Boolean = false,
    )

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val THEME = intPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_colors")
        val ACCENT = stringPreferencesKey("accent")
        val FONT_SIZE = intPreferencesKey("font_size")
        val DATE_FORMAT = intPreferencesKey("date_format")
        val WEEK_START = intPreferencesKey("week_start")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val NUMBER_FORMAT = intPreferencesKey("number_format")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val LOCK_DELAY = intPreferencesKey("lock_delay")
        val HIDE_BALANCE = booleanPreferencesKey("hide_balance")
        val NOTIFY_BUDGET = booleanPreferencesKey("notify_budget")
        val NOTIFY_BILL = booleanPreferencesKey("notify_bill")
        val NOTIFY_SUBS = booleanPreferencesKey("notify_subs")
        val NOTIFY_LOANS = booleanPreferencesKey("notify_loans")
        val NOTIFY_DAILY = booleanPreferencesKey("notify_daily")
        val DAILY_TIME = stringPreferencesKey("daily_reminder_time")
        val NOTIFY_ACH = booleanPreferencesKey("notify_achievements")
        val FIRST_RUN = booleanPreferencesKey("first_run_complete")
        val ONBOARDING = booleanPreferencesKey("onboarding_seen")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            currencySymbol = prefs[Keys.CURRENCY] ?: "₹",
            themeMode = ThemeMode.fromOrdinal(prefs[Keys.THEME] ?: 0),
            dynamicColors = prefs[Keys.DYNAMIC] ?: true,
            accent = Accent.fromName(prefs[Keys.ACCENT]),
            fontSize = FontSize.fromOrdinal(prefs[Keys.FONT_SIZE] ?: 1),
            dateFormat = DateFormat.fromOrdinal(prefs[Keys.DATE_FORMAT] ?: 0),
            weekStart = WeekStart.entries.getOrElse(prefs[Keys.WEEK_START] ?: 0) { WeekStart.MONDAY },
            monthStartDay = (prefs[Keys.MONTH_START_DAY] ?: 1).coerceIn(1, 31),
            numberFormat = NumberFormat.entries.getOrElse(prefs[Keys.NUMBER_FORMAT] ?: 0) { NumberFormat.INDIAN },
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            lockDelay = LockDelay.entries.getOrElse(prefs[Keys.LOCK_DELAY] ?: 0) { LockDelay.IMMEDIATELY },
            hideBalanceDefault = prefs[Keys.HIDE_BALANCE] ?: false,
            notifyBudget = prefs[Keys.NOTIFY_BUDGET] ?: true,
            notifyBill = prefs[Keys.NOTIFY_BILL] ?: true,
            notifySubscriptions = prefs[Keys.NOTIFY_SUBS] ?: true,
            notifyLoans = prefs[Keys.NOTIFY_LOANS] ?: true,
            notifyDaily = prefs[Keys.NOTIFY_DAILY] ?: false,
            dailyReminderTime = prefs[Keys.DAILY_TIME] ?: "21:00",
            notifyAchievements = prefs[Keys.NOTIFY_ACH] ?: true,
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

    suspend fun setAccent(accent: Accent) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    suspend fun setFontSize(size: FontSize) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size.ordinal }
    }

    suspend fun setDateFormat(format: DateFormat) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = format.ordinal }
    }

    suspend fun setWeekStart(start: WeekStart) {
        context.dataStore.edit { it[Keys.WEEK_START] = start.ordinal }
    }

    suspend fun setMonthStartDay(day: Int) {
        context.dataStore.edit { it[Keys.MONTH_START_DAY] = day.coerceIn(1, 31) }
    }

    suspend fun setNumberFormat(format: NumberFormat) {
        context.dataStore.edit { it[Keys.NUMBER_FORMAT] = format.ordinal }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    suspend fun setLockDelay(delay: LockDelay) {
        context.dataStore.edit { it[Keys.LOCK_DELAY] = delay.ordinal }
    }

    suspend fun setHideBalanceDefault(hidden: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_BALANCE] = hidden }
    }

    suspend fun setNotifyBudget(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_BUDGET] = enabled }
    }

    suspend fun setNotifyBill(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_BILL] = enabled }
    }

    suspend fun setNotifySubscriptions(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_SUBS] = enabled }
    }

    suspend fun setNotifyLoans(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_LOANS] = enabled }
    }

    suspend fun setNotifyDaily(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_DAILY] = enabled }
    }

    suspend fun setDailyReminderTime(time: String) {
        context.dataStore.edit { it[Keys.DAILY_TIME] = time }
    }

    suspend fun setNotifyAchievements(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ACH] = enabled }
    }

    /** Resets every setting to its default (used by Clear All Data). */
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
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
