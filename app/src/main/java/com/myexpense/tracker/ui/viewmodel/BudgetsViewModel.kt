package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetStatus
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.BudgetRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BudgetFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    OVER_BUDGET("Over Budget"),
    COMPLETED("Completed"),
}

data class BudgetsUiState(
    val budgets: List<BudgetWithSpent> = emptyList(),
    val filter: BudgetFilter = BudgetFilter.ALL,
    val totalBudgeted: Long = 0,
    val totalSpent: Long = 0,
    val remaining: Long = 0,
    val overallProgress: Float = 0f,
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "$",
    val error: String? = null,
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val achievementUnlocker: AchievementUnlocker,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(BudgetFilter.ALL)
    private val error = MutableStateFlow<String?>(null)

    private data class Side(
        val categories: List<Category>,
        val symbol: String,
        val error: String?,
    )

    private val side = combine(
        categoryRepository.observeByType(com.myexpense.tracker.data.model.TransactionType.EXPENSE),
        settingsRepository.settings,
        error,
    ) { cats, settings, err -> Side(cats, settings.currencySymbol, err) }

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.observeBudgetsWithSpent(),
        filter,
        side,
    ) { budgets, f, s ->
        val filtered = when (f) {
            BudgetFilter.ALL -> budgets
            BudgetFilter.ACTIVE -> budgets.filter { it.status == BudgetStatus.ACTIVE }
            BudgetFilter.OVER_BUDGET -> budgets.filter { it.status == BudgetStatus.OVER_BUDGET }
            BudgetFilter.COMPLETED -> budgets.filter { it.status == BudgetStatus.COMPLETED }
        }
        val budgeted = filtered.sumOf { it.budget.limitAmount }
        val spent = filtered.sumOf { it.spent }
        BudgetsUiState(
            budgets = filtered,
            filter = f,
            totalBudgeted = budgeted,
            totalSpent = spent,
            remaining = budgeted - spent,
            overallProgress = if (budgeted > 0) (spent.toFloat() / budgeted).coerceIn(0f, 1f) else 0f,
            categories = s.categories,
            currencySymbol = s.symbol,
            error = s.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun setFilter(f: BudgetFilter) { filter.value = f }

    fun save(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.save(budget)
            achievementUnlocker.onBudgetCreated()
            error.value = null
        }
    }

    fun delete(budget: Budget) {
        viewModelScope.launch { budgetRepository.delete(budget.id) }
    }

    fun markCompleted(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.markCompleted(budget.id)
        }
    }

    fun markActive(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.markActive(budget.id)
        }
    }

    fun clearError() { error.value = null }
}
