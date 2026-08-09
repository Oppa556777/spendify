package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.BudgetRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.data.repository.currentMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val month: YearMonth = currentMonth(),
    val income: Long = 0,
    val expense: Long = 0,
    val totalBalance: Long = 0,
    val recentTransactions: List<Transaction> = emptyList(),
    val budgets: List<BudgetWithSpent> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    budgetRepository: BudgetRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private data class Money(val income: Long, val expense: Long, val balance: Long)
    private data class Content(
        val recent: List<Transaction>,
        val budgets: List<BudgetWithSpent>,
        val symbol: String,
    )

    private val money = combine(
        transactionRepository.observeIncomeForMonth(currentMonth()),
        transactionRepository.observeExpenseForMonth(currentMonth()),
        accountRepository.observeTotalBalance(),
    ) { income, expense, balance -> Money(income, expense, balance) }

    private val content = combine(
        transactionRepository.observeRecent(6),
        budgetRepository.observeForMonthWithSpent(currentMonth()),
        settingsRepository.settings,
    ) { recent, budgets, settings -> Content(recent, budgets, settings.currencySymbol) }

    val uiState: StateFlow<HomeUiState> = combine(money, content) { m, c ->
        HomeUiState(
            income = m.income,
            expense = m.expense,
            totalBalance = m.balance,
            recentTransactions = c.recent,
            budgets = c.budgets,
            currencySymbol = c.symbol,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
