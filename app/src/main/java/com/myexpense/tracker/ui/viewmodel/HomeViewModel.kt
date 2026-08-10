package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.DailyStat
import com.myexpense.tracker.data.model.GoalWithProgress
import com.myexpense.tracker.data.model.HomePeriod
import com.myexpense.tracker.data.model.SubscriptionReminder
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.BudgetRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.GoalRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.SubscriptionRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.data.repository.currentMonth
import com.myexpense.tracker.utils.endMillis
import com.myexpense.tracker.utils.startMillis
import com.myexpense.tracker.utils.toEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "Good Morning 👋",
    val dateText: String = "",
    val period: HomePeriod = HomePeriod.THIS_MONTH,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val currencySymbol: String = "$",
    val balanceHidden: Boolean = false,
    val totalBalance: Long = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val dailyStats: List<DailyStat> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgets: List<BudgetWithSpent> = emptyList(),
    val goals: List<GoalWithProgress> = emptyList(),
    val subscriptionsDue: List<SubscriptionReminder> = emptyList(),
    val notificationCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val subscriptionRepository: SubscriptionRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val period = MutableStateFlow(HomePeriod.THIS_MONTH)
    private val customFrom = MutableStateFlow<LocalDate?>(null)
    private val customTo = MutableStateFlow<LocalDate?>(null)
    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val balanceHidden = MutableStateFlow(false)

    private data class Window(val from: Long, val to: Long)

    private val window: kotlinx.coroutines.flow.Flow<Window> =
        combine(period, customFrom, customTo) { p, cf, ct ->
            when (p) {
                HomePeriod.TODAY -> {
                    val t = LocalDate.now()
                    Window(t.toEpochMillis(), t.toEpochMillis())
                }
                HomePeriod.THIS_WEEK -> {
                    val t = LocalDate.now()
                    val start = t.minusDays((t.dayOfWeek.value - 1).toLong())
                    Window(start.toEpochMillis(), t.toEpochMillis())
                }
                HomePeriod.THIS_MONTH -> {
                    val m = YearMonth.now()
                    Window(m.startMillis(), m.endMillis())
                }
                HomePeriod.LAST_MONTH -> {
                    val m = YearMonth.now().minusMonths(1)
                    Window(m.startMillis(), m.endMillis())
                }
                HomePeriod.THIS_YEAR -> {
                    val y = LocalDate.now().year
                    Window(LocalDate.of(y, 1, 1).toEpochMillis(), LocalDate.now().toEpochMillis())
                }
                HomePeriod.CUSTOM -> {
                    val f = cf ?: YearMonth.now().atDay(1)
                    val t = ct ?: LocalDate.now()
                    Window(
                        minOf(f, t).toEpochMillis(),
                        maxOf(f, t).toEpochMillis(),
                    )
                }
            }
        }

    private val money = combine(window, selectedAccountId) { w, acc -> w to acc }
        .flatMapLatest { (w, acc) ->
            transactionRepository.observePeriodTotals(w.from, w.to, acc)
        }

    private val balances = accountRepository.observeActiveWithBalance()

    private val daily = transactionRepository.observeDailySeries(7)
    private val recent = transactionRepository.observeRecent(10)
    private val budgets = budgetRepository.observeForMonthWithSpent(currentMonth())
    private val goals = goalRepository.observeAll()
    private val due = subscriptionRepository.observeDueWithin(7)
    private val categories = categoryRepository.observeAll()

    private data class Filters(
        val period: HomePeriod,
        val customFrom: LocalDate?,
        val customTo: LocalDate?,
        val accountId: Long?,
    )

    private data class Side(
        val balances: List<AccountWithBalance>,
        val total: Long,
        val income: Long,
        val expense: Long,
    )

    private data class Content(
        val daily: List<DailyStat>,
        val recent: List<Transaction>,
        val budgets: List<BudgetWithSpent>,
        val goals: List<GoalWithProgress>,
        val due: List<SubscriptionReminder>,
        val categories: List<Category>,
        val symbol: String,
        val hidden: Boolean,
    )

    private data class ContentPart1(
        val daily: List<DailyStat>,
        val recent: List<Transaction>,
        val budgets: List<BudgetWithSpent>,
        val goals: List<GoalWithProgress>,
        val due: List<SubscriptionReminder>,
    )

    private data class ContentPart2(
        val categories: List<Category>,
        val symbol: String,
        val hidden: Boolean,
    )

    private val filters = combine(period, customFrom, customTo, selectedAccountId) { p, cf, ct, acc ->
        Filters(p, cf, ct, acc)
    }

    private val side = combine(balances, selectedAccountId, money) { list, acc, m ->
        Side(
            balances = list,
            total = if (acc == null) list.sumOf { it.balance } else list.firstOrNull { it.account.id == acc }?.balance ?: 0L,
            income = m.first,
            expense = m.second,
        )
    }

    private val contentPart1 = combine(daily, recent, budgets, goals, due) { d, r, b, g, s ->
        ContentPart1(d, r, b, g, s)
    }

    private val contentPart2 = combine(categories, settingsRepository.settings, balanceHidden) { c, st, h ->
        ContentPart2(c, st.currencySymbol, h)
    }

    private val content = combine(contentPart1, contentPart2) { p1, p2 ->
        Content(
            daily = p1.daily,
            recent = p1.recent,
            budgets = p1.budgets,
            goals = p1.goals,
            due = p1.due,
            categories = p2.categories,
            symbol = p2.symbol,
            hidden = p2.hidden,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(filters, side, content) { f, s, c ->
        HomeUiState(
            greeting = greetingFor(LocalTime.now().hour),
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault())),
            period = f.period,
            customFrom = f.customFrom,
            customTo = f.customTo,
            currencySymbol = c.symbol,
            balanceHidden = c.hidden,
            totalBalance = s.total,
            income = s.income,
            expense = s.expense,
            accounts = s.balances,
            categories = c.categories,
            selectedAccountId = f.accountId,
            dailyStats = c.daily,
            recentTransactions = c.recent,
            budgets = c.budgets,
            goals = c.goals,
            subscriptionsDue = c.due,
            notificationCount = c.due.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setPeriod(p: HomePeriod) { period.value = p }

    fun setCustomRange(from: LocalDate, to: LocalDate) {
        customFrom.value = from
        customTo.value = to
        period.value = HomePeriod.CUSTOM
    }

    fun selectAccount(id: Long?) { selectedAccountId.value = id }

    fun toggleBalanceVisibility() { balanceHidden.value = !balanceHidden.value }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }

    fun markSubscriptionNoted(reminder: SubscriptionReminder) {
        viewModelScope.launch { subscriptionRepository.markNoted(reminder.id) }
    }

    private fun greetingFor(hour: Int): String = when (hour) {
        in 5..11 -> "Good Morning 👋"
        in 12..16 -> "Good Afternoon 👋"
        in 17..20 -> "Good Evening 👋"
        else -> "Good Night 🌙"
    }
}
