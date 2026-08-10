package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalBalance: Long = 0,
    val currencySymbol: String = "$",
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeActiveWithBalance(),
        accountRepository.observeTotalBalance(),
        settingsRepository.settings,
        error,
    ) { accounts, total, settings, err ->
        AccountsUiState(
            accounts = accounts,
            totalBalance = total,
            currencySymbol = settings.currencySymbol,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun save(account: Account) {
        viewModelScope.launch {
            accountRepository.save(account)
            error.value = null
        }
    }

    fun delete(account: Account) {
        viewModelScope.launch {
            val ok = accountRepository.delete(account.id)
            error.value = if (ok) {
                null
            } else {
                "Cannot delete the last account. Add another account first."
            }
        }
    }

    fun clearError() { error.value = null }
}
