package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Loan
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.LoanRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val totalLent: Long = 0,
    val totalBorrowed: Long = 0,
    val currencySymbol: String = "$",
)

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val achievementUnlocker: AchievementUnlocker,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<LoansUiState> = combine(
        loanRepository.observeAll(),
        loanRepository.observeTotals(),
        settingsRepository.settings,
    ) { loans, totals, settings ->
        LoansUiState(
            loans = loans,
            totalLent = totals.first,
            totalBorrowed = totals.second,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState())

    fun save(loan: Loan) {
        viewModelScope.launch { loanRepository.save(loan) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { loanRepository.delete(id) }
    }

    fun markSettled(id: Long) {
        viewModelScope.launch {
            loanRepository.markSettled(id)
            achievementUnlocker.onLoanSettled()
        }
    }

    fun addPayment(id: Long, amountMinor: Long) {
        viewModelScope.launch { loanRepository.addPayment(id, amountMinor) }
    }
}
