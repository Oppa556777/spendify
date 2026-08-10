package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Accent
import com.myexpense.tracker.data.model.DateFormat
import com.myexpense.tracker.data.model.FontSize
import com.myexpense.tracker.data.model.LockDelay
import com.myexpense.tracker.data.model.NumberFormat
import com.myexpense.tracker.data.model.ThemeMode
import com.myexpense.tracker.data.model.WeekStart
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<SettingsRepository.Settings> =
        settingsRepository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsRepository.Settings(),
        )

    fun setCurrency(symbol: String) = launch { settingsRepository.setCurrencySymbol(symbol) }
    fun setThemeMode(mode: ThemeMode) = launch {
        settingsRepository.setThemeMode(mode)
        syncGlobals()
    }
    fun setDynamicColors(enabled: Boolean) = launch { settingsRepository.setDynamicColors(enabled) }
    fun setAccent(accent: Accent) = launch { settingsRepository.setAccent(accent) }
    fun setFontSize(size: FontSize) = launch { settingsRepository.setFontSize(size) }
    fun setDateFormat(format: DateFormat) = launch {
        settingsRepository.setDateFormat(format)
        syncGlobals()
    }
    fun setWeekStart(start: WeekStart) = launch { settingsRepository.setWeekStart(start) }
    fun setMonthStartDay(day: Int) = launch { settingsRepository.setMonthStartDay(day) }
    fun setNumberFormat(format: NumberFormat) = launch {
        settingsRepository.setNumberFormat(format)
        syncGlobals()
    }
    fun setBiometricEnabled(enabled: Boolean) = launch { settingsRepository.setBiometricEnabled(enabled) }
    fun setLockDelay(delay: LockDelay) = launch { settingsRepository.setLockDelay(delay) }
    fun setHideBalanceDefault(hidden: Boolean) = launch { settingsRepository.setHideBalanceDefault(hidden) }
    fun setNotifyBudget(enabled: Boolean) = launch { settingsRepository.setNotifyBudget(enabled) }
    fun setNotifyBill(enabled: Boolean) = launch { settingsRepository.setNotifyBill(enabled) }
    fun setNotifySubscriptions(enabled: Boolean) = launch { settingsRepository.setNotifySubscriptions(enabled) }
    fun setNotifyLoans(enabled: Boolean) = launch { settingsRepository.setNotifyLoans(enabled) }
    fun setNotifyDaily(enabled: Boolean) = launch { settingsRepository.setNotifyDaily(enabled) }
    fun setDailyReminderTime(time: String) = launch { settingsRepository.setDailyReminderTime(time) }
    fun setNotifyAchievements(enabled: Boolean) = launch { settingsRepository.setNotifyAchievements(enabled) }

    /** Push locale/format preferences into the global formatters. */
    private fun syncGlobals() {
        val s = settings.value
        MoneyFormatter.useIndianFormat = s.numberFormat == NumberFormat.INDIAN
        DateUtils.activeDateFormat = s.dateFormat
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
