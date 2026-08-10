package com.myexpense.tracker.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.data.model.DailyStat
import com.myexpense.tracker.data.model.GoalWithProgress
import com.myexpense.tracker.data.model.HomePeriod
import com.myexpense.tracker.data.model.SubscriptionReminder
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// HERO BALANCE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeroBalanceCard(
    greeting: String,
    dateText: String,
    balance: Long,
    symbol: String,
    hidden: Boolean,
    income: Long,
    expense: Long,
    accountPages: List<Pair<Long?, String>>,
    notificationCount: Int,
    onPageChange: (Int) -> Unit,
    onToggleVisibility: () -> Unit,
    onBell: () -> Unit,
    onSearch: () -> Unit,
    onAvatar: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { accountPages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    LaunchedEffect(accountPages.size) {
        if (pagerState.currentPage >= accountPages.size && accountPages.isNotEmpty()) {
            pagerState.scrollToPage(accountPages.size - 1)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3B82F6))))
            .statusBarsPadding(),
    ) {
        // ── Greeting + icons ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Box {
                IconButton(onClick = onBell) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = notificationCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp, start = 4.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(onClick = onAvatar),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }

        // ── Balance ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total Balance",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle balance visibility",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
            if (hidden) {
                Text(
                    text = "$symbol ••••••",
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    color = Color.White,
                    modifier = Modifier.blur(6.dp),
                )
            } else {
                Text(
                    text = MoneyFormatter.formatIndian(balance, symbol),
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }

        // ── Income / expense mini cards ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeroMiniCard(
                label = "Income",
                amount = MoneyFormatter.format(income, 0),
                symbol = symbol,
                icon = Icons.Filled.ArrowUpward,
                iconColor = Color(0xFF69F0AE),
                modifier = Modifier.weight(1f),
            )
            HeroMiniCard(
                label = "Expenses",
                amount = MoneyFormatter.format(expense, 0),
                symbol = symbol,
                icon = Icons.Filled.ArrowDownward,
                iconColor = Color(0xFFFF8A80),
                modifier = Modifier.weight(1f),
            )
        }

        // ── Account switcher (swipeable) ───────────────────────────────────
        if (accountPages.size > 1) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) { page ->
                Text(
                    text = accountPages[page].second,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                accountPages.indices.forEach { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.4f))
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMiniCard(
    label: String,
    amount: String,
    symbol: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.18f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = "$symbol$amount",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERIOD FILTER CHIPS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PeriodFilterChips(
    selected: HomePeriod,
    onSelect: (HomePeriod) -> Unit,
) {
    val periods = HomePeriod.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.horizontalScroll(rememberScrollState()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { period ->
                    val isSelected = selected == period
                    Surface(
                        modifier = Modifier.clickable { onSelect(period) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                        border = if (!isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        } else null,
                    ) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MINI BAR CHART (last 7 days)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniBarChart(
    stats: List<DailyStat>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(700)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val safeMax = maxOf(stats.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1L, 1L)
    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { 180.dp.toPx() }
        val labelH = with(density) { 20.dp.toPx() }
        val topPad = with(density) { 20.dp.toPx() }
        val chartH = heightPx - labelH - topPad
        val slotW = if (stats.isEmpty()) 0f else widthPx / stats.size
        val barW = minOf(slotW * 0.28f, with(density) { 16.dp.toPx() })

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stats) {
                    detectTapGestures { offset ->
                        if (stats.isNotEmpty()) {
                            val index = (offset.x / (widthPx / stats.size)).toInt().coerceIn(0, stats.size - 1)
                            selected = if (selected == index) null else index
                        }
                    }
                },
        ) {
            if (stats.isEmpty()) return@Canvas

            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1f,
            )

            labelPaint.textAlign = android.graphics.Paint.Align.RIGHT
            labelPaint.textSize = with(density) { 11.dp.toPx() }
            labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                MoneyFormatter.formatCompact(safeMax, symbol),
                size.width,
                topPad - with(density) { 4.dp.toPx() },
                labelPaint,
            )

            stats.forEachIndexed { index, stat ->
                val cx = index * slotW + slotW / 2
                val isToday = index == stats.lastIndex
                val incomeH = if (safeMax > 0) stat.income.toFloat() / safeMax * chartH * progress.value else 0f
                val expenseH = if (safeMax > 0) stat.expense.toFloat() / safeMax * chartH * progress.value else 0f

                val incomeColor = if (isToday) Color(0xFF69F0AE) else Color(0xFF4CAF50)
                val expenseColor = if (isToday) Color(0xFFB388FF) else Color(0xFF6C63FF)

                if (incomeH > 0f) {
                    drawRoundRect(
                        color = incomeColor,
                        topLeft = Offset(cx - barW - 1f, chartH - incomeH),
                        size = Size(barW, incomeH),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
                if (expenseH > 0f) {
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = Offset(cx + 1f, chartH - expenseH),
                        size = Size(barW, expenseH),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
                labelPaint.textAlign = android.graphics.Paint.Align.CENTER
                labelPaint.textSize = with(density) { 11.dp.toPx() }
                labelPaint.color = if (isToday) {
                    MaterialTheme.colorScheme.primary.toArgb()
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                }
                drawContext.canvas.nativeCanvas.drawText(
                    stat.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    cx,
                    size.height - with(density) { 4.dp.toPx() },
                    labelPaint,
                )
            }
        }

        // Tooltip
        selected?.let { index ->
            if (index in stats.indices) {
                val stat = stats[index]
                val cx = index * slotW + slotW / 2
                val pillWidth = with(density) { 128.dp.toPx() }
                val x = (cx - pillWidth / 2).coerceIn(0f, widthPx - pillWidth)
                Surface(
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE61A1035),
                    shadowElevation = 4.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "↑ $symbol${MoneyFormatter.format(stat.income)}",
                            color = Color(0xFF69F0AE),
                            fontSize = 11.sp,
                            fontFamily = AmountFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "↓ $symbol${MoneyFormatter.format(stat.expense)}",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            fontFamily = AmountFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK ACTIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickActionsRow(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onReports: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(
            icon = Icons.Filled.ArrowDownward,
            label = "Add Expense",
            color = Color(0xFFE53935),
            onClick = onExpense,
        )
        QuickAction(
            icon = Icons.Filled.ArrowUpward,
            label = "Add Income",
            color = Color(0xFF43A047),
            onClick = onIncome,
        )
        QuickAction(
            icon = Icons.Filled.SwapHoriz,
            label = "Transfer",
            color = Color(0xFF3B82F6),
            onClick = onTransfer,
        )
        QuickAction(
            icon = Icons.Filled.BarChart,
            label = "Reports",
            color = Color(0xFF6C63FF),
            onClick = onReports,
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BUDGET PROGRESS CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BudgetProgressCardView(
    budget: BudgetWithSpent,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = budget.progress
    val barColor = when {
        progress > 1f -> Color(0xFFB71C1C)
        progress > 0.8f -> Color(0xFFF44336)
        progress > 0.6f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "budgetProgress",
    )
    val category = budget.category
    val color = Color(category?.color ?: 0xFF6C63FF)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(icon = category?.icon, color = color, size = 40)
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = category?.name ?: "All categories",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (budget.isOverspent) {
                        Text(
                            text = "Over Budget!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$symbol${MoneyFormatter.format(budget.spent)} / $symbol${MoneyFormatter.format(budget.limitAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth(),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            Text(
                text = "${(progress * 100).toInt().coerceAtLeast(0)}%",
                style = MaterialTheme.typography.labelLarge,
                color = barColor,
                fontWeight = FontWeight.Bold,
                fontFamily = AmountFontFamily,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SWIPEABLE TRANSACTION ROW
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTransactionRow(
    transaction: Transaction,
    symbol: String,
    categoryName: String,
    categoryIcon: String?,
    categoryColor: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF3B82F6)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                },
            ) {
                Icon(
                    imageVector = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icons.Filled.Edit
                    } else {
                        Icons.Filled.Delete
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(24.dp),
                )
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionRow(
                title = transaction.title.ifBlank { categoryName },
                subtitle = listOfNotNull(
                    categoryName,
                    DateUtils.shortDate(transaction.date) + if (transaction.time.isNotBlank()) " • ${transaction.time}" else "",
                ).joinToString("  •  "),
                icon = categoryIcon,
                iconColor = categoryColor,
                amount = transaction.amount,
                symbol = symbol,
                isExpense = transaction.isExpense,
                onClick = onClick,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRANSACTION DETAIL SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    symbol: String,
    categoryName: String,
    categoryIcon: String?,
    categoryColor: Color,
    accountName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryIcon(icon = categoryIcon, color = categoryColor, size = 44)
                Column {
                    Text(
                        text = transaction.title.ifBlank { categoryName },
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = listOfNotNull(categoryName, accountName).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = (if (transaction.isExpense) "-" else "+") + "$symbol" + MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isExpense) Color(0xFFE53935) else Color(0xFF43A047),
            )
            val detailLines = buildString {
                append(DateUtils.fullDate(transaction.date))
                if (transaction.time.isNotBlank()) append(" • ").append(transaction.time)
                if (transaction.note.isNotBlank()) append("\n").append(transaction.note)
            }
            Text(
                text = detailLines,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  Edit")
                }
                Button(
                    onClick = onDelete,
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

// ─────────────────────────────────────────────────────────────────────────────
// GOAL CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GoalCardView(
    goal: GoalWithProgress,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val color = Color(goal.goal.color)
    val progress = goal.progress.coerceIn(0f, 1f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.goal.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$symbol${MoneyFormatter.format(goal.goal.savedAmount)} / $symbol${MoneyFormatter.format(goal.goal.targetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${goal.percent}% achieved",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                goal.goal.deadline?.let {
                    Text(
                        text = "By ${DateUtils.shortDate(it.toLocalDate())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800),
                    )
                }
            }
            ProgressRing(progress = progress, color = color, size = 62.dp)
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    color: Color,
    size: Dp,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.toPx() * 0.12f
            drawArc(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUBSCRIPTION REMINDER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SubscriptionReminderCard(
    reminder: SubscriptionReminder,
    symbol: String,
    onDismiss: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = reminder.daysLeft
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFE0B2),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "📅", fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${reminder.name} $symbol${MoneyFormatter.format(reminder.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF4E342E),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "due in $days day${if (days == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D4C41),
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xFF6D4C41))
            }
            Button(
                onClick = onPay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text("Pay", color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeSectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(4.dp),
            )
        }
    }
}

/** Converts an epoch-millis deadline to a LocalDate for display. */
private fun Long.toLocalDate(): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate()
