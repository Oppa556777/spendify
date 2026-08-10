package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.data.repository.currentMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class TransactionsUiState(
    val month: YearMonth = currentMonth(),
    val typeFilter: TransactionType? = null,
    val categoryFilter: Long? = null,
    val accountFilter: Long? = null,
    val query: String = "",
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(currentMonth())
    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val accountFilter = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    private val filters = combine(month, typeFilter, categoryFilter, accountFilter, query) { m, t, c, a, q ->
        FilterArgs(m, t, c, a, q)
    }

    private val filteredTransactions = filters.flatMapLatest { args ->
        transactionRepository.observeFiltered(args.month, args.type, args.categoryId, args.accountId, args.query)
    }

    private val monthIncome = month.flatMapLatest { transactionRepository.observeIncomeForMonth(it) }
    private val monthExpense = month.flatMapLatest { transactionRepository.observeExpenseForMonth(it) }

    private data class ListPart(
        val args: FilterArgs,
        val list: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>,
    )

    private data class MoneyPart(
        val income: Long,
        val expense: Long,
        val symbol: String,
    )

    private val listPart = combine(
        filters,
        filteredTransactions,
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
    ) { args, list, categories, accounts ->
        ListPart(args, list, categories, accounts)
    }

    private val moneyPart = combine(monthIncome, monthExpense, settingsRepository.settings) { income, expense, settings ->
        MoneyPart(income, expense, settings.currencySymbol)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(listPart, moneyPart) { lp, mp ->
        TransactionsUiState(
            month = lp.args.month,
            typeFilter = lp.args.type,
            categoryFilter = lp.args.categoryId,
            accountFilter = lp.args.accountId,
            query = lp.args.query,
            transactions = lp.list,
            categories = lp.categories,
            accounts = lp.accounts,
            currencySymbol = mp.symbol,
            monthIncome = mp.income,
            monthExpense = mp.expense,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setMonth(newMonth: YearMonth) { month.value = newMonth }
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun setTypeFilter(type: TransactionType?) { typeFilter.value = type }
    fun setCategoryFilter(id: Long?) { categoryFilter.value = id }
    fun setAccountFilter(id: Long?) { accountFilter.value = id }
    fun setQuery(q: String) { query.value = q }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }

    private data class FilterArgs(
        val month: YearMonth,
        val type: TransactionType?,
        val categoryId: Long?,
        val accountId: Long?,
        val query: String,
    )
}
