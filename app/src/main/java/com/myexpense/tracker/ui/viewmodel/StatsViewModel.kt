package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.CategoryStat
import com.myexpense.tracker.data.model.MonthlyPoint
import com.myexpense.tracker.data.model.TransactionType
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
import java.time.YearMonth
import javax.inject.Inject

data class StatsUiState(
    val month: YearMonth = currentMonth(),
    val income: Long = 0,
    val expense: Long = 0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val trend: List<MonthlyPoint> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(currentMonth())

    private val categoryStats = month.flatMapLatest { m ->
        transactionRepository.observeCategoryStats(m.toString(), TransactionType.EXPENSE)
    }

    private val trend = month.flatMapLatest { m ->
        transactionRepository.observeMonthlySeries(m.minusMonths(11).atDay(1), m.atEndOfMonth())
    }

    private val income = month.flatMapLatest { transactionRepository.observeIncomeForMonth(it) }
    private val expense = month.flatMapLatest { transactionRepository.observeExpenseForMonth(it) }

    private data class Money(val income: Long, val expense: Long, val month: YearMonth)
    private data class Details(
        val stats: List<CategoryStat>,
        val trend: List<MonthlyPoint>,
        val categories: List<Category>,
        val symbol: String,
    )

    private val money = combine(month, income, expense) { m, inc, exp -> Money(inc, exp, m) }

    private val details = combine(
        categoryStats,
        trend,
        categoryRepository.observeAll(),
        settingsRepository.settings,
    ) { s, t, c, settings -> Details(s, t, c, settings.currencySymbol) }

    val uiState: StateFlow<StatsUiState> = combine(money, details) { m, d ->
        val categoryMap = d.categories.associateBy { it.id }
        val total = d.stats.sumOf { it.total }
        val withFractions = d.stats.map {
            it.copy(fraction = if (total > 0) it.total.toFloat() / total else 0f).also { stat ->
                stat.category = stat.categoryId?.let(categoryMap::get)
            }
        }
        StatsUiState(
            month = m.month,
            income = m.income,
            expense = m.expense,
            categoryStats = withFractions,
            trend = d.trend,
            categories = d.categories,
            currencySymbol = d.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setMonth(newMonth: YearMonth) { month.value = newMonth }
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
}
