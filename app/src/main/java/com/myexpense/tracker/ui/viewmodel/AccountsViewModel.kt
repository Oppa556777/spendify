package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeActiveWithBalance(),
        accountRepository.observeTotalBalance(),
        settingsRepository.settings,
    ) { accounts, total, settings ->
        AccountsUiState(
            accounts = accounts,
            totalBalance = total,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun save(account: Account) {
        viewModelScope.launch { accountRepository.save(account) }
    }

    fun delete(account: Account) {
        viewModelScope.launch { accountRepository.delete(account.id) }
    }
}
