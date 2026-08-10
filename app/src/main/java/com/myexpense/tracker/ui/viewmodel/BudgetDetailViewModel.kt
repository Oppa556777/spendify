package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.BudgetRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetDetailUiState(
    val budget: BudgetWithSpent? = null,
    val dailySeries: List<Pair<LocalDate, Long>> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val budgetId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val budgetFlow: Flow<BudgetWithSpent?> = budgetRepository.observeBudgetWithSpent(budgetId)
    private val dailySeries: Flow<List<Pair<LocalDate, Long>>> = budgetRepository.observeBudgetDailySeries(budgetId)

    private val transactions: Flow<List<Transaction>> = budgetFlow.flatMapLatest { bws ->
        if (bws == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            val (wf, wt) = budgetRepository.budgetWindow(bws.budget)
            if (bws.budget.categoryId == null) {
                transactionRepository.observeBetween(wf, wt).map { list ->
                    list.filter { it.type == TransactionType.EXPENSE }
                }
            } else {
                transactionRepository.observeExpensesForCategories(bws.budget.categoryIds, wf, wt)
            }
        }
    }

    private data class Main(
        val budget: BudgetWithSpent?,
        val series: List<Pair<LocalDate, Long>>,
        val txs: List<Transaction>,
    )

    private data class Ref(
        val categories: List<Category>,
        val symbol: String,
    )

    private val main = combine(budgetFlow, dailySeries, transactions) { b, s, t -> Main(b, s, t) }
    private val ref = combine(categoryRepository.observeAll(), settingsRepository.settings) { c, s -> Ref(c, s.currencySymbol) }

    val uiState: StateFlow<BudgetDetailUiState> = combine(main, ref) { m, r ->
        BudgetDetailUiState(
            budget = m.budget,
            dailySeries = m.series,
            transactions = m.txs,
            categories = r.categories,
            currencySymbol = r.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetDetailUiState())

    fun save(budget: Budget) {
        viewModelScope.launch { budgetRepository.save(budget) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { budgetRepository.delete(id) }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }
}
