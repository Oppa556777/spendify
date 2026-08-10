package com.myexpense.tracker.ui.screens.accounts

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.AccountDetailViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    onBack: () -> Unit,
    onTransfer: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    val account = state.account
    val categoryMap = state.categories.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (account == null) {
            EmptyState(title = "Account not found", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Hero card ──────────────────────────────────────────────────
            item {
                AccountHeroCard(account = account, balance = account.balance, symbol = symbol)
            }

            // ── Transfer button ────────────────────────────────────────────
            item {
                Button(
                    onClick = { onTransfer(account.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Transfer From/To This Account")
                }
            }

            // ── Balance chart ───────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Balance · Last 30 days",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (state.balanceSeries.isEmpty() || state.balanceSeries.all { it.second == 0L }) {
                            Text(
                                text = "No activity in the last 30 days.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        } else {
                            BalanceLineChart(
                                series = state.balanceSeries,
                                symbol = symbol,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                            )
                        }
                    }
                }
            }

            // ── Filter tabs ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("All") },
                    )
                    FilterChip(
                        selected = state.filter == TransactionType.INCOME,
                        onClick = { viewModel.setFilter(TransactionType.INCOME) },
                        label = { Text("Income") },
                    )
                    FilterChip(
                        selected = state.filter == TransactionType.EXPENSE,
                        onClick = { viewModel.setFilter(TransactionType.EXPENSE) },
                        label = { Text("Expense") },
                    )
                }
            }

            // ── Transaction list ────────────────────────────────────────────
            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No transactions",
                        subtitle = "Nothing matches this filter for this account.",
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { t ->
                    val category = t.categoryId?.let { categoryMap[it] }
                    val isTransfer = t.type == TransactionType.TRANSFER
                    val color = when {
                        isTransfer -> Color(0xFF3B82F6)
                        t.isExpense -> Color(0xFFE53935)
                        else -> Color(0xFF43A047)
                    }
                    TransactionRow(
                        title = t.title.ifBlank {
                            when {
                                isTransfer -> "Transfer"
                                t.isExpense -> "Expense"
                                else -> "Income"
                            }
                        },
                        subtitle = listOfNotNull(
                            category?.name ?: if (isTransfer) "Transfer" else "Uncategorized",
                            DateUtils.shortDate(t.date) + if (t.time.isNotBlank()) " • ${t.time}" else "",
                        ).joinToString("  •  "),
                        icon = if (isTransfer) "swap_horiz" else category?.icon,
                        iconColor = color,
                        amount = t.amount,
                        symbol = symbol,
                        isExpense = t.isExpense || isTransfer,
                        onClick = null,
                        trailing = {
                            if (isTransfer) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = "Transfer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountHeroCard(
    account: com.myexpense.tracker.data.model.Account,
    balance: Long,
    symbol: String,
) {
    val gradient = remember(account.id) {
        listOf(
            Color(account.color),
            blend(account.color, 0.4f),
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(20.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Text(
                            text = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.25f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (account.type.name == "CREDIT_CARD") {
                                    Icons.Filled.ArrowUpward
                                } else {
                                    Icons.Filled.ArrowDownward
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "$symbol${MoneyFormatter.format(balance)}",
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    color = Color.White,
                )
                Text(
                    text = if (account.type.name == "CREDIT_CARD") "Outstanding" else "Available balance",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun blend(color: Long, factor: Float): Color {
    val c = Color(color)
    return Color(
        red = c.red * (1f - factor),
        green = c.green * (1f - factor),
        blue = c.blue * (1f - factor),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BALANCE LINE CHART (30 days)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BalanceLineChart(
    series: List<Pair<java.time.LocalDate, Long>>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(series) { progress.snapTo(0f); progress.animateTo(1f, tween(700)) }
    val density = LocalDensity.current
    var selected by remember { mutableStateOf<Int?>(null) }

    val values = series.map { it.second }
    val minVal = values.minOrNull() ?: 0L
    val maxVal = values.maxOrNull() ?: 0L
    val range = (maxVal - minVal).coerceAtLeast(1L)
    val n = values.size

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

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
            val topPad = with(density) { 8.dp.toPx() }
            val chartH = heightPx - topPad - with(density) { 4.dp.toPx() }
            val stepX = if (n > 1) size.width / (n - 1) else size.width

            fun yFor(v: Long) = topPad + chartH - (v - minVal).toFloat() / range * chartH

            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, yFor(0)),
                end = Offset(size.width, yFor(0)),
                strokeWidth = 1f,
            )

            val visibleCount = (progress.value * n).toInt().coerceIn(1, n)
            val linePath = Path()
            (0 until visibleCount).forEach { i ->
                val p = Offset(i * stepX, yFor(values[i]))
                if (i == 0) linePath.moveTo(p.x, p.y) else linePath.lineTo(p.x, p.y)
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo((visibleCount - 1) * stepX, yFor(0))
                lineTo(0f, yFor(0))
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF6C63FF).copy(alpha = 0.28f), Color.Transparent),
                    startY = topPad,
                    endY = yFor(0),
                ),
            )
            drawPath(
                path = linePath,
                color = Color(0xFF6C63FF),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            (0 until visibleCount).forEach { i ->
                drawCircle(
                    color = if (selected == i) Color(0xFFB388FF) else Color(0xFF6C63FF).copy(alpha = 0.7f),
                    radius = if (selected == i) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(i * stepX, yFor(values[i])),
                )
            }
        }

        // tooltip
        selected?.let { index ->
            if (index in series.indices) {
                val stepX = if (n > 1) widthPx / (n - 1) else widthPx
                val pillW = with(density) { 160.dp.toPx() }
                val x = ((index * stepX) - pillW / 2).coerceIn(0f, (widthPx - pillW).coerceAtLeast(0f))
                Surface(
                    modifier = Modifier.padding(start = x.dpToPxCompat(density)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE61A1035),
                ) {
                    Text(
                        text = "${series[index].first}  $symbol${MoneyFormatter.format(series[index].second)}",
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

private fun Float.dpToPxCompat(density: androidx.compose.ui.unit.Density): androidx.compose.ui.unit.Dp =
    with(density) { this@dpToPxCompat.toDp() }
