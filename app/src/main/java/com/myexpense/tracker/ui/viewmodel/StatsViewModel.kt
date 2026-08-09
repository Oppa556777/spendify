package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.database.dao.CategorySumRow
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
import kotlinx.coroutines.flow.map
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
        transactionRepository.observeCategoryStats(m.toString(), TransactionType.EXPENSE).map { rows ->
            rows.map { row -> row.toStat() }
        }
    }

    private val trend = month.flatMapLatest { m ->
        val from = m.minusMonths(11).atDay(1)
        val to = m.atEndOfMonth()
        transactionRepository.observeMonthlySeries(from, to).map { rows ->
            buildTrend(m, rows)
        }
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

    private val details = combine(categoryStats, trend, categoryRepository.observeAll(), settingsRepository.settings) { s, t, c, settings ->
        Details(s, t, c, settings.currencySymbol)
    }

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

    private fun CategorySumRow.toStat(): CategoryStat = CategoryStat(
        categoryId = categoryId,
        total = total,
        count = count,
    )

    private fun buildTrend(selected: YearMonth, rows: List<com.myexpense.tracker.data.database.dao.MonthSumRow>): List<MonthlyPoint> {
        val byMonth = rows.groupBy { it.month }
        return (0L..11L).map { offset ->
            val m = selected.minusMonths(offset)
            val key = m.toString()
            val income = byMonth[key]?.filter { it.type == TransactionType.INCOME }?.sumOf { it.total } ?: 0L
            val expense = byMonth[key]?.filter { it.type == TransactionType.EXPENSE }?.sumOf { it.total } ?: 0L
            MonthlyPoint(month = m, income = income, expense = expense)
        }.reversed()
    }
}
