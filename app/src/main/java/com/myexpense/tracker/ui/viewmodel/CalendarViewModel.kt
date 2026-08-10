package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.DailyStat
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<DailyStat> = emptyList(),      // every day of the month (incl. zero)
    val selectedDate: LocalDate? = null,
    val dayTransactions: List<Transaction> = emptyList(),
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    private val days = month.flatMapLatest { m ->
        transactionRepository.observeDailyRange(m.atDay(1), m.atEndOfMonth())
    }

    private val dayTransactions = combine(month, selectedDate) { m, d -> m to d }
        .flatMapLatest { (m, d) ->
            val date = d ?: LocalDate.now()
            if (YearMonth.from(date) != m) kotlinx.coroutines.flow.flowOf(emptyList())
            else transactionRepository.observeBetween(date, date)
        }

    private val monthIncome = month.flatMapLatest { transactionRepository.observeIncomeForMonth(it) }
    private val monthExpense = month.flatMapLatest { transactionRepository.observeExpenseForMonth(it) }

    private data class Core(
        val month: YearMonth,
        val days: List<DailyStat>,
        val selectedDate: LocalDate?,
        val txs: List<Transaction>,
        val income: Long,
        val expense: Long,
    )

    private data class Money(
        val income: Long,
        val expense: Long,
    )

    private val money = combine(monthIncome, monthExpense) { i, e -> Money(i, e) }

    private val core = combine(month, days, selectedDate, dayTransactions, money) {
        m, d, s, t, mo -> Core(m, d, s, t, mo.income, mo.expense)
    }

    val uiState: StateFlow<CalendarUiState> = combine(core, settingsRepository.settings) { c, s ->
        CalendarUiState(
            month = c.month,
            days = c.days,
            selectedDate = c.selectedDate,
            dayTransactions = c.txs,
            monthExpense = c.expense,
            monthIncome = c.income,
            currencySymbol = s.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() { month.value = month.value.minusMonths(1); selectedDate.value = null }
    fun nextMonth() { month.value = month.value.plusMonths(1); selectedDate.value = null }
    fun select(date: LocalDate) { selectedDate.value = if (selectedDate.value == date) null else date }
    fun clearSelection() { selectedDate.value = null }
}
