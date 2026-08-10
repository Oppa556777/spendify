package com.myexpense.tracker.ui.screens.budgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.BudgetDetailViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: BudgetDetailViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    val categoryMap = state.categories.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.budget?.budget?.name?.ifBlank { state.budget?.category?.name ?: "Budget" } ?: "Budget",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (state.budget != null) {
                        IconButton(onClick = { showSheet = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { pendingDelete = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val bws = state.budget
        if (bws == null) {
            EmptyState(title = "Budget not found", modifier = Modifier.padding(padding))
        } else {
            val budget = bws.budget
            val accent = Color(budget.colorHex.removePrefix("#").toLongOrNull(16)?.let { 0xFF000000 or it } ?: 0xFF6C63FF)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── Summary: donut spent vs remaining ───────────────────────
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            BudgetDetailDonut(
                                spent = bws.spent,
                                limit = budget.limitAmount,
                                color = accent,
                                modifier = Modifier.size(120.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (bws.isOverspent) "Over budget!" else "On track",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bws.isOverspent) Color(0xFFB71C1C) else Color(0xFF43A047),
                                )
                                DetailStat("Spent", "$symbol${MoneyFormatter.format(bws.spent)}", Color(0xFFE53935))
                                DetailStat("Budget", "$symbol${MoneyFormatter.format(budget.limitAmount)}", accent)
                                DetailStat(
                                    "Remaining",
                                    "$symbol${MoneyFormatter.format(bws.remaining)}",
                                    if (bws.remaining >= 0) Color(0xFF43A047) else Color(0xFFB71C1C),
                                )
                                Text(
                                    text = "Period: ${budget.period.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // ── Day-by-day spending line ────────────────────────────────
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Day-by-day spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = NunitoFamily,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                            if (state.dailySeries.isEmpty() || state.dailySeries.all { it.second == 0L }) {
                                Text(
                                    text = "No spending in this period yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 20.dp),
                                )
                            } else {
                                BudgetDayChart(
                                    series = state.dailySeries,
                                    symbol = symbol,
                                    color = accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                )
                            }
                        }
                    }
                }

                // ── Transactions ────────────────────────────────────────────
                item {
                    Text(
                        text = "Transactions (${state.transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (state.transactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No transactions",
                            subtitle = "Expenses in this budget's categories will appear here.",
                        )
                    }
                } else {
                    items(state.transactions, key = { it.id }) { t ->
                        val category = t.categoryId?.let { categoryMap[it] }
                        TransactionRow(
                            title = t.title.ifBlank { category?.name ?: "Expense" },
                            subtitle = listOfNotNull(
                                category?.name,
                                DateUtils.shortDate(t.date) + if (t.time.isNotBlank()) " • ${t.time}" else "",
                            ).joinToString("  •  "),
                            icon = category?.icon,
                            iconColor = Color(category?.color ?: 0xFFE53935),
                            amount = t.amount,
                            symbol = symbol,
                            isExpense = true,
                            onClick = { onEditTransaction(t.id) },
                        )
                    }
                }

                // ── Edit / Delete actions ───────────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showSheet = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  Edit")
                        }
                        Button(
                            onClick = { pendingDelete = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  Delete")
                        }
                    }
                }
            }
        }
    }

    if (showSheet && state.budget != null) {
        AddEditBudgetSheet(
            budget = state.budget.budget,
            categories = state.categories.filter { it.type == com.myexpense.tracker.data.model.TransactionType.EXPENSE },
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { budget ->
                viewModel.save(budget)
                showSheet = false
            },
        )
    }

    if (pendingDelete && state.budget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("Delete budget?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(state.budget!!.budget.id)
                    pendingDelete = false
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DONUT (spent vs remaining)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetDetailDonut(
    spent: Long,
    limit: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(spent, limit) { anim.snapTo(0f); anim.animateTo(1f, tween(800)) }
    val fraction = if (limit > 0) (spent.toFloat() / limit).coerceIn(0f, 1f) else 0f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.13f
            val inset = stroke / 2
            drawArc(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = if (fraction > 1f) Color(0xFFB71C1C) else color,
                startAngle = -90f,
                sweepAngle = 360f * fraction * anim.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAY-BY-DAY LINE CHART
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetDayChart(
    series: List<Pair<java.time.LocalDate, Long>>,
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(series) { anim.snapTo(0f); anim.animateTo(1f, tween(800)) }
    val density = LocalDensity.current
    var selected by remember { mutableStateOf<Int?>(null) }

    val values = series.map { it.second }
    val maxVal = values.maxOrNull() ?: 1L
    val n = values.size

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val topPad = with(density) { 6.dp.toPx() }
        val chartH = heightPx - topPad - with(density) { 4.dp.toPx() }
        val stepX = if (n > 1) widthPx / (n - 1) else widthPx

        fun yFor(v: Long) = topPad + chartH - (if (maxVal > 0) v.toFloat() / maxVal * chartH else 0f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        if (n > 0) {
                            val index = (offset.x / (widthPx / n)).toInt().coerceIn(0, n - 1)
                            selected = if (selected == index) null else index
                        }
                    }
                },
        ) {
            if (n == 0) return@Canvas
            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, yFor(0)),
                end = Offset(size.width, yFor(0)),
                strokeWidth = 1f,
            )
            val visible = (anim.value * n).toInt().coerceIn(1, n)
            val path = Path()
            (0 until visible).forEach { i ->
                val p = Offset(i * stepX, yFor(values[i]))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            (0 until visible).forEach { i ->
                drawCircle(
                    color = if (selected == i) color.copy(alpha = 1f) else color.copy(alpha = 0.55f),
                    radius = if (selected == i) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(i * stepX, yFor(values[i])),
                )
            }
        }

        selected?.let { index ->
            if (index in series.indices) {
                val pillW = with(density) { 150.dp.toPx() }
                val x = ((index * stepX) - pillW / 2).coerceIn(0f, (widthPx - pillW).coerceAtLeast(0f))
                Surface(
                    modifier = Modifier.padding(start = with(density) { x.toDp() }),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE61A1035),
                ) {
                    Text(
                        text = "${DateUtils.shortDate(series[index].first)}  $symbol${MoneyFormatter.format(series[index].second)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
