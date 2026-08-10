package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AccountDetailUiState(
    val account: Account? = null,
    val balanceSeries: List<Pair<LocalDate, Long>> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val filter = MutableStateFlow<TransactionType?>(null)

    private val account: Flow<Account?> = accountRepository.observeById(accountId)

    private val balanceSeries: Flow<List<Pair<LocalDate, Long>>> = account.flatMapLatest { acc ->
        transactionRepository.observeAccountBalanceSeries(accountId, acc?.balance ?: 0L)
    }

    private val transactions = combine(filter, account) { f, acc ->
        f to acc
    }.flatMapLatest { (f, acc) ->
        if (acc == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else transactionRepository.observeForAccount(accountId, f)
    }

    private data class MainPart(
        val account: Account?,
        val series: List<Pair<LocalDate, Long>>,
        val txs: List<Transaction>,
        val filter: TransactionType?,
    )

    private data class RefPart(
        val categories: List<Category>,
        val symbol: String,
    )

    private val mainPart = combine(account, balanceSeries, transactions, filter) { acc, series, txs, f ->
        MainPart(acc, series, txs, f)
    }

    private val refPart = combine(categoryRepository.observeAll(), settingsRepository.settings) { cats, settings ->
        RefPart(cats, settings.currencySymbol)
    }

    val uiState: StateFlow<AccountDetailUiState> = combine(mainPart, refPart) { m, r ->
        AccountDetailUiState(
            account = m.account,
            balanceSeries = m.series,
            transactions = m.txs,
            filter = m.filter,
            categories = r.categories,
            currencySymbol = r.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())

    fun setFilter(f: TransactionType?) { filter.value = f }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }
}
