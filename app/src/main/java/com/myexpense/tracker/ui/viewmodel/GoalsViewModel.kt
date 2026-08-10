package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Goal
import com.myexpense.tracker.data.model.GoalWithProgress
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.GoalRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeAll(),
        accountRepository.observeActive(),
        settingsRepository.settings,
    ) { goals, accounts, settings ->
        GoalsUiState(
            goals = goals,
            accounts = accounts,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun save(goal: Goal) {
        viewModelScope.launch { goalRepository.save(goal) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { goalRepository.delete(id) }
    }

    fun addMoney(id: Long, amountMinor: Long) {
        viewModelScope.launch { goalRepository.addMoney(id, amountMinor) }
    }
}
