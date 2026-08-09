package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.BudgetRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
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

data class BudgetsUiState(
    val month: YearMonth = currentMonth(),
    val budgets: List<BudgetWithSpent> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "$",
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(currentMonth())

    private val budgets = month.flatMapLatest { budgetRepository.observeForMonthWithSpent(it) }

    val uiState: StateFlow<BudgetsUiState> = combine(
        month,
        budgets,
        categoryRepository.observeByType(TransactionType.EXPENSE),
        settingsRepository.settings,
    ) { m, b, cats, settings ->
        BudgetsUiState(
            month = m,
            budgets = b,
            categories = cats,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun setMonth(newMonth: YearMonth) { month.value = newMonth }
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun save(budget: Budget) {
        viewModelScope.launch { budgetRepository.save(budget) }
    }

    fun delete(budget: Budget) {
        viewModelScope.launch { budgetRepository.delete(budget.id) }
    }
}
