package com.myexpense.tracker.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.CategoryStat
import com.myexpense.tracker.data.model.MonthlyPoint
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.MonthSelector
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.viewmodel.StatsViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Statistics", style = MaterialTheme.typography.titleLarge)
                androidx.compose.material3.IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("INCOME", style = MaterialTheme.typography.labelSmall)
                        Text(
                            MoneyFormatter.formatWithSymbol(state.income, symbol),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontFamily = AmountFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("EXPENSES", style = MaterialTheme.typography.labelSmall)
                        Text(
                            MoneyFormatter.formatWithSymbol(state.expense, symbol),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontFamily = AmountFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            Text("Spending by category", style = MaterialTheme.typography.titleMedium)
        }

        if (state.categoryStats.isEmpty()) {
            item {
                EmptyState(
                    title = "No expenses this month",
                    subtitle = "Add expenses to see your category breakdown.",
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        stats = state.categoryStats,
                        modifier = Modifier.size(190.dp),
                    )
                }
            }

            items(state.categoryStats, key = { it.categoryId ?: -1 }) { stat ->
                CategoryBreakdownRow(stat = stat, symbol = symbol)
            }
        }

        item {
            Text("Last 12 months", style = MaterialTheme.typography.titleMedium)
        }

        item {
            TrendChart(
                points = state.trend,
                symbol = symbol,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Income")
                LegendItem(color = MaterialTheme.colorScheme.error, label = "Expense")
            }
        }
    }
}

@Composable
private fun DonutChart(
    stats: List<CategoryStat>,
    modifier: Modifier = Modifier,
) {
    val total = stats.sumOf { it.total }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.14f
            var startAngle = -90f
            stats.forEach { stat ->
                val sweep = if (total > 0) stat.total.toFloat() / total * 360f else 0f
                val color = Color(stat.category?.color ?: 0xFF9E9E9E)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = MoneyFormatter.formatCompact(total, ""),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    stat: CategoryStat,
    symbol: String,
) {
    val color = Color(stat.category?.color ?: 0xFF9E9E9E)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(icon = stat.category?.icon, color = color, size = 38)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stat.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${(stat.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    MoneyFormatter.formatWithSymbol(stat.total, symbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text("  $label", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TrendChart(
    points: List<MonthlyPoint>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        if (points.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries {
                series(*points.map { it.income.toFloat() }.toFloatArray())
                series(*points.map { it.expense.toFloat() }.toFloatArray())
            }
        }
    }

    if (points.isEmpty()) {
        EmptyState(title = "No data yet", subtitle = "Add transactions to see trends.")
        return
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    listOf(
                        rememberLineComponent(fill(MaterialTheme.colorScheme.primary), 8.dp),
                        rememberLineComponent(fill(MaterialTheme.colorScheme.error), 8.dp),
                    )
                ),
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val index = value.toInt().coerceIn(0, points.lastIndex)
                    DateUtils.shortMonthYear(points[index].month)
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
