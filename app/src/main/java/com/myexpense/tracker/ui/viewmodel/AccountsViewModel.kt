package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountSummary(
    val assets: Long = 0,       // sum of positive non-credit-card balances
    val liabilities: Long = 0,  // sum of credit-card balances (debt)
    val netWorth: Long = 0,     // assets - liabilities
)

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val summary: AccountSummary = AccountSummary(),
    val latestTransactions: Map<Long, Transaction> = emptyMap(),
    val currencySymbol: String = "$",
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    private data class Money(val summary: AccountSummary, val symbol: String)

    private val money = combine(
        accountRepository.observeActiveWithBalance(),
        settingsRepository.settings,
    ) { accounts, settings ->
        val assets = accounts
            .filter { it.account.type != AccountType.CREDIT_CARD && it.balance > 0 }
            .sumOf { it.balance }
        val liabilities = accounts
            .filter { it.account.type == AccountType.CREDIT_CARD }
            .sumOf { it.balance.coerceAtLeast(0) }
        Money(
            summary = AccountSummary(
                assets = assets,
                liabilities = liabilities,
                netWorth = assets - liabilities,
            ),
            symbol = settings.currencySymbol,
        )
    }

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeActiveWithBalance(),
        transactionRepository.observeLatestPerAccount(),
        money,
        error,
    ) { accounts, latest, m, err ->
        val latestMap = latest.associateBy { it.accountId ?: it.toAccountId ?: -1L }
        AccountsUiState(
            accounts = accounts,
            summary = m.summary,
            latestTransactions = latestMap,
            currencySymbol = m.symbol,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun save(account: Account) {
        viewModelScope.launch {
            // Only one default account allowed.
            if (account.isDefault) {
                accountRepository.getActive().filter { it.id != account.id && it.isDefault }
                    .forEach { accountRepository.save(it.copy(isDefault = false)) }
            }
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
