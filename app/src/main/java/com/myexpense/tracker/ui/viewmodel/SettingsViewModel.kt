package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.ThemeMode
import com.myexpense.tracker.data.repository.SettingsRepository
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

    fun setCurrency(symbol: String) {
        viewModelScope.launch { settingsRepository.setCurrencySymbol(symbol) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }
}
