package com.myexpense.tracker.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.ReportsPeriod
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.ReportsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    val snackbar = remember { SnackbarHostState() }
    var showCustomPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let(viewModel::exportPdf) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportCsv) }

    var donutSelection by remember { mutableStateOf<Int?>(null) }
    var pieSelection by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reports & Analytics",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Period selector ─────────────────────────────────────────────
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReportsPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = state.period == period,
                            onClick = {
                                if (period == ReportsPeriod.CUSTOM) {
                                    showCustomPicker = true
                                } else {
                                    viewModel.setPeriod(period)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportsPeriod.entries.size),
                            label = { Text(period.label) },
                        )
                    }
                }
            }

            // ── Date navigation ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::previous) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    Text(
                        text = state.windowLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = viewModel::next) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
            }

            // ── Chart 1: Income vs Expense ──────────────────────────────────
            item {
                ChartCard(title = "Income vs Expense") {
                    if (state.barBuckets.isEmpty() || state.barBuckets.all { it.income == 0L && it.expense == 0L }) {
                        ChartEmpty()
                    } else {
                        IncomeExpenseBarChart(buckets = state.barBuckets, symbol = symbol)
                        ChartLegend(
                            listOf(
                                "● Income" to Color(0xFF4CAF50),
                                "● Expense" to Color(0xFFE53935),
                            )
                        )
                    }
                }
            }

            // ── Chart 2: Expense donut ──────────────────────────────────────
            item {
                ChartCard(title = "Expense by Category") {
                    if (state.expenseByCategory.isEmpty()) {
                        ChartEmpty()
                    } else {
                        ExpenseDonutChart(
                            stats = state.expenseByCategory,
                            symbol = symbol,
                            selectedIndex = donutSelection,
                            onSelect = { donutSelection = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        // breakdown for the selected (or top) category
                        val shown = donutSelection?.let { state.expenseByCategory.getOrNull(it) }
                            ?: state.expenseByCategory.firstOrNull()
                        shown?.let { stat ->
                            CategoryBreakdownRow(stat = stat, symbol = symbol)
                        }
                    }
                }
            }

            // ── Chart 3: Daily spending line ────────────────────────────────
            item {
                ChartCard(title = "Daily Spending Trend") {
                    if (state.dailyCurrent.all { it == 0L } && state.dailyPrevious.all { it == 0L }) {
                        ChartEmpty()
                    } else {
                        DailySpendingLineChart(
                            current = state.dailyCurrent,
                            previous = state.dailyPrevious,
                            labels = state.dailyLabels,
                            symbol = symbol,
                        )
                        ChartLegend(
                            listOf(
                                "● This month" to Color(0xFF6C63FF),
                                "- - Last month" to Color.Gray,
                            )
                        )
                    }
                }
            }

            // ── Chart 4: Heatmap ────────────────────────────────────────────
            item {
                ChartCard(title = "Spending Heatmap · ${state.heatmapLabel}") {
                    if (state.heatmap.isEmpty() || state.heatmap.all { it.expense == 0L }) {
                        ChartEmpty()
                    } else {
                        SpendingHeatmap(
                            cells = state.heatmap,
                            monthLabel = state.heatmapLabel,
                            symbol = symbol,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HeatLegendDot(Color(0xFFD1C4E9))
                            HeatLegendDot(Color(0xFFB39DDB))
                            HeatLegendDot(Color(0xFF7E57C2))
                            HeatLegendDot(Color(0xFF6A1B9A))
                            HeatLegendDot(Color(0xFFB71C1C))
                            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Chart 5: Category trends ────────────────────────────────────
            item {
                ChartCard(title = "Category Trends · Last 6 months") {
                    if (state.categoryTrends.isEmpty()) {
                        ChartEmpty()
                    } else {
                        CategoryTrendChart(
                            trends = state.categoryTrends,
                            months = state.trendMonths,
                            symbol = symbol,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.categoryTrends.forEachIndexed { index, trend ->
                                LegendItem(
                                    label = trend.name,
                                    color = trendPaletteFor(index),
                                )
                            }
                        }
                    }
                }
            }

            // ── Chart 6: Income sources ─────────────────────────────────────
            item {
                ChartCard(title = "Income Sources") {
                    if (state.incomeSources.isEmpty()) {
                        ChartEmpty()
                    } else {
                        IncomeSourcePie(
                            stats = state.incomeSources,
                            symbol = symbol,
                            selectedIndex = pieSelection,
                            onSelect = { pieSelection = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        state.incomeSources.forEach { stat ->
                            CategoryBreakdownRow(stat = stat, symbol = symbol, showFraction = true)
                        }
                    }
                }
            }

            // ── Chart 7: Savings rate gauge ─────────────────────────────────
            item {
                ChartCard(title = "Savings Rate") {
                    if (state.income == 0L && state.expense == 0L) {
                        ChartEmpty()
                    } else {
                        SavingsRateGauge(
                            rate = state.savingsRate,
                            income = state.income,
                            expense = state.expense,
                            symbol = symbol,
                        )
                    }
                }
            }

            // ── Chart 8: Net worth ──────────────────────────────────────────
            item {
                ChartCard(title = "Net Worth · Last 12 months") {
                    if (state.netWorth.isEmpty() || state.netWorth.all { it.value == 0L }) {
                        ChartEmpty()
                    } else {
                        NetWorthChart(points = state.netWorth, symbol = symbol)
                    }
                }
            }

            // ── Chart 9: Top spending ───────────────────────────────────────
            item {
                ChartCard(title = "Top 5 Spending Categories") {
                    if (state.topSpending.isEmpty()) {
                        ChartEmpty()
                    } else {
                        TopSpendingBars(items = state.topSpending, symbol = symbol)
                    }
                }
            }

            // ── Chart 10: Cash flow waterfall ───────────────────────────────
            item {
                ChartCard(title = "Monthly Cash Flow") {
                    if (state.cashFlow.income == 0L && state.cashFlow.expense == 0L && state.cashFlow.closing == 0L) {
                        ChartEmpty()
                    } else {
                        CashFlowWaterfall(flow = state.cashFlow, symbol = symbol)
                        ChartLegend(
                            listOf(
                                "Opening" to Color(0xFF78909C),
                                "+ Income" to Color(0xFF4CAF50),
                                "- Expenses" to Color(0xFFE53935),
                                "Closing" to Color(0xFF3B82F6),
                            )
                        )
                    }
                }
            }

            // ── Export ──────────────────────────────────────────────────────
            item {
                ChartCard(title = "Export") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                pdfLauncher.launch("moneymate-report-${state.windowLabel.replace(" ", "-")}.pdf")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Export PDF")
                        }
                        Button(
                            onClick = {
                                csvLauncher.launch("moneymate-report-${state.windowLabel.replace(" ", "-")}.csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Export CSV")
                        }
                        Text(
                            text = "Reports are saved to the Downloads folder on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Custom range picker ────────────────────────────────────────────────
    if (showCustomPicker) {
        var stage by remember { mutableStateOf(0) }
        var from by remember { mutableStateOf<LocalDate?>(null) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (stage == 0) {
                            from = date
                            stage = 1
                        } else {
                            from?.let { f -> viewModel.setCustomRange(minOf(f, date), maxOf(f, date)) }
                            showCustomPicker = false
                        }
                    }
                }) { Text(if (stage == 0) "From" else "To") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState, title = { Text(if (stage == 0) "Start date" else "End date") })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chart card + helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ChartEmpty() {
    EmptyState(
        title = "No data yet",
        subtitle = "Add transactions to see this chart.",
    )
}

@Composable
private fun ChartLegend(items: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items.forEach { (label, color) ->
            LegendItem(label = label, color = color)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(0.dp),
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
        }
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeatLegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .padding(0.dp),
    ) {
        Surface(shape = RoundedCornerShape(3.dp), color = color, modifier = Modifier.size(10.dp)) {}
    }
}

@Composable
private fun CategoryBreakdownRow(
    stat: com.myexpense.tracker.data.model.CategoryStat,
    symbol: String,
    showFraction: Boolean = false,
) {
    val color = Color(stat.category?.color ?: 0xFF9E9E9E)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.size(8.dp)) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
        }
        Text(
            text = stat.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$symbol${com.myexpense.tracker.utils.MoneyFormatter.format(stat.total)}" +
                if (showFraction) "  ${(stat.fraction * 100).toInt()}%" else "",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = com.myexpense.tracker.ui.theme.AmountFontFamily,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Palette used for the category trend lines (matches the chart). */
private fun trendPaletteFor(index: Int): Color = when (index % 5) {
    0 -> Color(0xFF6C63FF)
    1 -> Color(0xFF3B82F6)
    2 -> Color(0xFFEC407A)
    3 -> Color(0xFFFF9800)
    else -> Color(0xFF26A69A)
}
