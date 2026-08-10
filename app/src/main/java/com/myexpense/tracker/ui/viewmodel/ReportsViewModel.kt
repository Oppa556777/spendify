package com.myexpense.tracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.BarBucket
import com.myexpense.tracker.data.model.CashFlow
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.CategoryStat
import com.myexpense.tracker.data.model.CategoryTrend
import com.myexpense.tracker.data.model.HeatCell
import com.myexpense.tracker.data.model.MonthlyPoint
import com.myexpense.tracker.data.model.NetWorthPoint
import com.myexpense.tracker.data.model.ReportsPeriod
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.ExportRepository
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class ReportsUiState(
    val period: ReportsPeriod = ReportsPeriod.MONTH,
    val windowLabel: String = "",
    val currencySymbol: String = "₹",
    // period totals + derived
    val income: Long = 0,
    val expense: Long = 0,
    val savingsRate: Float = 0f,
    val cashFlow: CashFlow = CashFlow(0, 0, 0, 0),
    // charts
    val barBuckets: List<BarBucket> = emptyList(),
    val expenseByCategory: List<CategoryStat> = emptyList(),
    val incomeSources: List<CategoryStat> = emptyList(),
    val dailyCurrent: List<Long> = emptyList(),
    val dailyPrevious: List<Long> = emptyList(),
    val dailyLabels: List<String> = emptyList(),
    val heatmap: List<HeatCell> = emptyList(),
    val heatmapLabel: String = "",
    val categoryTrends: List<CategoryTrend> = emptyList(),
    val trendMonths: List<String> = emptyList(),
    val netWorth: List<NetWorthPoint> = emptyList(),
    val topSpending: List<CategoryStat> = emptyList(),
    val categories: List<Category> = emptyList(),
    val exportMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val exportRepository: ExportRepository,
    private val achievementUnlocker: AchievementUnlocker,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(ReportsPeriod.MONTH)
    private val anchor = MutableStateFlow(LocalDate.now())
    private val customRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    private val exportMessage = MutableStateFlow<String?>(null)

    // ── Window ──────────────────────────────────────────────────────────────
    private data class Window(
        val from: LocalDate,
        val to: LocalDate,
        val label: String,
        val dailyBuckets: Boolean,
        val weekOfMonthBuckets: Boolean,
    )

    private val window: Flow<Window> = combine(period, anchor, customRange) { p, a, cr ->
        when (p) {
            ReportsPeriod.WEEK -> {
                val monday = a.minusDays((a.dayOfWeek.value - 1).toLong())
                val sunday = monday.plusDays(6)
                Window(
                    from = monday,
                    to = sunday,
                    label = "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${monday.month.name.lowercase()
                        .replaceFirstChar { it.uppercase() }}",
                    dailyBuckets = true,
                    weekOfMonthBuckets = false,
                )
            }
            ReportsPeriod.MONTH -> {
                val ym = YearMonth.from(a)
                Window(
                    from = ym.atDay(1),
                    to = ym.atEndOfMonth(),
                    label = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    dailyBuckets = false,
                    weekOfMonthBuckets = true,
                )
            }
            ReportsPeriod.YEAR -> {
                val y = a.year
                Window(
                    from = LocalDate.of(y, 1, 1),
                    to = LocalDate.of(y, 12, 31),
                    label = y.toString(),
                    dailyBuckets = false,
                    weekOfMonthBuckets = false,
                )
            }
            ReportsPeriod.CUSTOM -> {
                val range = cr ?: (YearMonth.from(a).atDay(1) to a)
                val from = minOf(range.first, range.second)
                val to = maxOf(range.first, range.second)
                val span = ChronoUnit.DAYS.between(from, to) + 1
                Window(
                    from = from,
                    to = to,
                    label = "${from.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – " +
                        to.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                    dailyBuckets = span <= 62,
                    weekOfMonthBuckets = false,
                )
            }
        }
    }

    // ── Base data ───────────────────────────────────────────────────────────
    private val totals = window.flatMapLatest { w ->
        transactionRepository.observePeriodTotals(w.from.toEpochMillis(), w.to.toEpochMillis(), null)
    }

    private val balance = accountRepository.observeTotalBalance()

    private val barBuckets: Flow<List<BarBucket>> = window.flatMapLatest { w ->
        when {
            w.dailyBuckets -> transactionRepository.observeDailyRange(w.from, w.to).map { days ->
                days.map { d ->
                    BarBucket(
                        label = d.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                        income = d.income,
                        expense = d.expense,
                    )
                }
            }
            w.weekOfMonthBuckets -> transactionRepository.observeDailyRange(w.from, w.to).map { days ->
                val buckets = (1..5).map { week -> BarBucket("W$week", 0, 0) }.toMutableList()
                days.forEach { d ->
                    val week = ((d.date.dayOfMonth - 1) / 7).coerceIn(0, 4)
                    buckets[week] = BarBucket(
                        label = buckets[week].label,
                        income = buckets[week].income + d.income,
                        expense = buckets[week].expense + d.expense,
                    )
                }
                buckets.filter { it.income > 0 || it.expense > 0 || true }
            }
            else -> transactionRepository.observeMonthlySeries(w.from, w.to).map { months ->
                months.map { m ->
                    BarBucket(
                        label = m.month.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())),
                        income = m.income,
                        expense = m.expense,
                    )
                }
            }
        }
    }

    private val expenseByCategory: Flow<List<CategoryStat>> = window.flatMapLatest { w ->
        transactionRepository.observeCategoryStatsBetween(w.from, w.to, TransactionType.EXPENSE)
    }

    private val incomeSources: Flow<List<CategoryStat>> = window.flatMapLatest { w ->
        transactionRepository.observeCategoryStatsBetween(w.from, w.to, TransactionType.INCOME)
    }

    // Daily spending line: current month vs previous month (fixed to today's month).
    private data class DailyLines(
        val current: List<Long>,
        val previous: List<Long>,
        val labels: List<String>,
    )

    private val dailyLines: Flow<DailyLines> = run {
        val cur = YearMonth.now()
        val prev = cur.minusMonths(1)
        combine(
            transactionRepository.observeDailyRange(prev.atDay(1), prev.atEndOfMonth()),
            transactionRepository.observeDailyRange(cur.atDay(1), cur.atEndOfMonth()),
        ) { p, c ->
            val length = cur.lengthOfMonth()
            val current = LongArray(length) { 0 }
            val previous = LongArray(length) { 0 }
            p.forEach { if (it.date.dayOfMonth <= length) previous[it.date.dayOfMonth - 1] = it.expense }
            c.forEach { if (it.date.dayOfMonth <= length) current[it.date.dayOfMonth - 1] = it.expense }
            DailyLines(
                current = current.toList(),
                previous = previous.toList(),
                labels = (1..length).map { it.toString() },
            )
        }
    }

    // Heatmap: the month containing the anchor.
    private val heatmap: Flow<List<HeatCell>> = anchor.flatMapLatest { a ->
        val ym = YearMonth.from(a)
        transactionRepository.observeDailyRange(ym.atDay(1), ym.atEndOfMonth()).map { days ->
            val max = days.maxOfOrNull { it.expense } ?: 0L
            days.map { d ->
                val level = if (max <= 0 || d.expense <= 0) 0 else {
                    val ratio = d.expense.toFloat() / max
                    when {
                        ratio <= 0.25f -> 1
                        ratio <= 0.5f -> 2
                        ratio <= 0.75f -> 3
                        else -> 4
                    }
                }
                HeatCell(date = d.date, expense = d.expense, level = level)
            }
        }
    }

    // Category trends: top 5 expense categories over the last 6 months.
    private data class TrendsData(
        val trends: List<CategoryTrend>,
        val monthLabels: List<String>,
    )

    private val categoryTrends: Flow<TrendsData> = anchor.flatMapLatest { a ->
        val endYm = YearMonth.from(a)
        val startYm = endYm.minusMonths(5)
        transactionRepository.observeMonthlyCategoryTotals(
            startYm.atDay(1),
            endYm.atEndOfMonth(),
            TransactionType.EXPENSE,
        )
    }.combine(categoryRepository.observeAll()) { rows, cats ->
        val anchorYm = YearMonth.from(anchor.value)
        val endYm = anchorYm
        val months = (0..5).map { endYm.minusMonths(5 - it.toLong()) }
        val monthLabels = months.map { it.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())) }
        val catMap = cats.associateBy { it.id }
        val byCategory = rows.groupBy { it.categoryId }
        val totalsByCat = byCategory.mapValues { (_, list) -> list.sumOf { it.total.toLong() } }
        val topIds = totalsByCat.entries.sortedByDescending { it.value }.take(5).map { it.key }
        val trends = topIds.map { cid ->
            val cat = cid?.let { catMap[it] }
            val values = months.map { m ->
                val key = m.toString()
                byCategory[cid]?.firstOrNull { it.month == key }?.total?.toLong() ?: 0L
            }
            CategoryTrend(
                name = cat?.name ?: "Uncategorized",
                color = cat?.color ?: 0xFF9E9E9E,
                values = values,
            )
        }
        TrendsData(trends, monthLabels)
    }

    // Net worth: last 12 months, computed backwards from the current balance.
    private val netWorth: Flow<List<NetWorthPoint>> = combine(anchor, balance) { a, bal ->
        a to bal
    }.flatMapLatest { (a, bal) ->
        val endYm = YearMonth.from(a)
        val startYm = endYm.minusMonths(11)
        transactionRepository.observeMonthlySeries(startYm.atDay(1), endYm.atEndOfMonth()).map { months ->
            val monthMap = months.associate { it.month to (it.income - it.expense) }
            val points = mutableListOf<NetWorthPoint>()
            var running = bal
            // iterate oldest → newest, but compute backwards from the end
            val nets = (0..11).map { off ->
                val m = endYm.minusMonths(11 - off.toLong())
                monthMap[m] ?: 0L
            }
            // running balance at end of month i = bal - sum(nets[j] for j>i)
            val suffix = LongArray(12)
            var acc = 0L
            for (i in 11 downTo 0) {
                acc += nets[i]
                suffix[i] = acc
            }
            (0..11).forEach { i ->
                val m = endYm.minusMonths(11 - i.toLong())
                points += NetWorthPoint(
                    label = m.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())),
                    value = bal - suffix[i],
                )
            }
            points
        }
    }

    // ── State assembly (grouped combines ≤5) ────────────────────────────────
    private data class PartA(
        val period: ReportsPeriod,
        val windowLabel: String,
        val income: Long,
        val expense: Long,
        val balance: Long,
    )

    private data class PartB(
        val bars: List<BarBucket>,
        val expenseCats: List<CategoryStat>,
        val incomeCats: List<CategoryStat>,
    )

    private data class PartC(
        val daily: DailyLines,
        val heat: List<HeatCell>,
        val heatLabel: String,
    )

    private data class PartD(
        val trends: List<CategoryTrend>,
        val trendMonths: List<String>,
        val net: List<NetWorthPoint>,
        val categories: List<Category>,
        val symbol: String,
    )

    private val partA = combine(window, totals, balance) { w, t, b ->
        PartA(period.value, w.label, t.first, t.second, b)
    }

    private val partB = combine(barBuckets, expenseByCategory, incomeSources) { b, e, i ->
        PartB(b, e, i)
    }

    private val partC = combine(dailyLines, heatmap, anchor) { d, h, a ->
        PartC(d, h, YearMonth.from(a).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())))
    }

    private val partD = combine(
        categoryTrends,
        netWorth,
        categoryRepository.observeAll(),
        settingsRepository.settings,
    ) { t, n, c, s ->
        PartD(t.trends, t.monthLabels, n, c, s.currencySymbol)
    }

    private val uiStateData = combine(partA, partB, partC, partD) { a, b, c, d ->
        val rate = if (a.income > 0) {
            ((a.income - a.expense).toFloat() / a.income).coerceIn(0f, 1f)
        } else 0f

        val catMap = d.categories.associateBy { it.id }
        fun resolve(list: List<CategoryStat>): List<CategoryStat> =
            list.map { stat ->
                stat.copy(fraction = 0f).also { st ->
                    st.category = st.categoryId?.let { catMap[it] }
                }
            }

        val expenseCats = resolve(b.expenseCats)
        val incomeCats = resolve(b.incomeCats)
        val totalExp = expenseCats.sumOf { it.total }
        val resolvedExpense = expenseCats.map {
            it.copy(fraction = if (totalExp > 0) it.total.toFloat() / totalExp else 0f)
        }
        val totalInc = incomeCats.sumOf { it.total }
        val resolvedIncome = incomeCats.map {
            it.copy(fraction = if (totalInc > 0) it.total.toFloat() / totalInc else 0f)
        }

        ReportsUiState(
            period = a.period,
            windowLabel = a.windowLabel,
            currencySymbol = d.symbol,
            income = a.income,
            expense = a.expense,
            savingsRate = rate,
            cashFlow = CashFlow(
                opening = a.balance - (a.income - a.expense),
                income = a.income,
                expense = a.expense,
                closing = a.balance,
            ),
            barBuckets = b.bars,
            expenseByCategory = resolvedExpense,
            incomeSources = resolvedIncome,
            dailyCurrent = c.daily.current,
            dailyPrevious = c.daily.previous,
            dailyLabels = c.daily.labels,
            heatmap = c.heat,
            heatmapLabel = c.heatLabel,
            categoryTrends = d.trends,
            trendMonths = d.trendMonths,
            netWorth = d.net,
            topSpending = resolvedExpense.take(5),
            categories = d.categories,
        )
    }

    val uiState: StateFlow<ReportsUiState> = combine(uiStateData, exportMessage) { s, em ->
        s.copy(exportMessage = em)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    // ── Navigation ──────────────────────────────────────────────────────────
    fun setPeriod(p: ReportsPeriod) {
        if (p != ReportsPeriod.CUSTOM) period.value = p
    }

    fun setCustomRange(from: LocalDate, to: LocalDate) {
        customRange.value = from to to
        period.value = ReportsPeriod.CUSTOM
    }

    fun next() {
        when (period.value) {
            ReportsPeriod.WEEK -> anchor.value = anchor.value.plusWeeks(1)
            ReportsPeriod.MONTH -> anchor.value = anchor.value.plusMonths(1)
            ReportsPeriod.YEAR -> anchor.value = anchor.value.plusYears(1)
            ReportsPeriod.CUSTOM -> Unit
        }
    }

    fun previous() {
        when (period.value) {
            ReportsPeriod.WEEK -> anchor.value = anchor.value.minusWeeks(1)
            ReportsPeriod.MONTH -> anchor.value = anchor.value.minusMonths(1)
            ReportsPeriod.YEAR -> anchor.value = anchor.value.minusYears(1)
            ReportsPeriod.CUSTOM -> Unit
        }
    }

    fun clearExportMessage() { exportMessage.value = null }

    // ── Exports ─────────────────────────────────────────────────────────────
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val symbol = uiState.value.currencySymbol
            val result = exportRepository.exportCsv(uri, null, symbol)
            if (result.isSuccess) achievementUnlocker.onExport()
            exportMessage.value = result.fold(
                onSuccess = { "CSV saved: $it transactions" },
                onFailure = { "CSV export failed: ${it.message}" },
            )
        }
    }

    fun exportPdf(uri: Uri) {
        viewModelScope.launch {
            val symbol = uiState.value.currencySymbol
            val month = YearMonth.from(anchor.value)
            val result = exportRepository.exportPdf(uri, month, symbol)
            if (result.isSuccess) achievementUnlocker.onExport()
            exportMessage.value = result.fold(
                onSuccess = { "PDF report saved ($it transactions)" },
                onFailure = { "PDF export failed: ${it.message}" },
            )
        }
    }
}
