package com.myexpense.tracker.ui.screens.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.data.model.BarBucket
import com.myexpense.tracker.data.model.CashFlow
import com.myexpense.tracker.data.model.CategoryStat
import com.myexpense.tracker.data.model.CategoryTrend
import com.myexpense.tracker.data.model.HeatCell
import com.myexpense.tracker.data.model.NetWorthPoint
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.utils.MoneyFormatter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val IncomeColor = Color(0xFF4CAF50)
private val IncomeColorBright = Color(0xFF69F0AE)
private val ExpenseColor = Color(0xFFE53935)
private val ExpenseColorSoft = Color(0xFFFF8A80)
private val TrendPurple = Color(0xFF6C63FF)
private val TrendBlue = Color(0xFF3B82F6)

/** Dark tooltip pill shown on chart taps. */
@Composable
private fun ChartTooltip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xE61A1035),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 1 — INCOME VS EXPENSE GROUPED BAR CHART
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomeExpenseBarChart(
    buckets: List<BarBucket>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets) { progress.snapTo(0f); progress.animateTo(1f, tween(700)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val maxVal = maxOf(buckets.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1L, 1L)

    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(220.dp)) { widthPx, heightPx ->
        val labelH = with(density) { 22.dp.toPx() }
        val topPad = with(density) { 22.dp.toPx() }
        val chartH = heightPx - labelH - topPad
        val slotW = if (buckets.isEmpty()) 0f else widthPx / buckets.size
        val barW = minOf(slotW * 0.30f, with(density) { 14.dp.toPx() })

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isNotEmpty()) {
                            val index = (offset.x / (widthPx / buckets.size)).toInt().coerceIn(0, buckets.size - 1)
                            selected = if (selected == index) null else index
                        }
                    }
                },
        ) {
            if (buckets.isEmpty()) return@Canvas
            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1f,
            )
            labelPaint.textSize = with(density) { 11.dp.toPx() }
            labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

            buckets.forEachIndexed { index, bucket ->
                val cx = index * slotW + slotW / 2
                val ih = if (maxVal > 0) bucket.income.toFloat() / maxVal * chartH * progress.value else 0f
                val eh = if (maxVal > 0) bucket.expense.toFloat() / maxVal * chartH * progress.value else 0f
                if (ih > 0f) {
                    drawRoundRect(
                        color = if (index == selected) IncomeColorBright else IncomeColor,
                        topLeft = Offset(cx - barW - 1f, chartH - ih),
                        size = Size(barW, ih),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
                if (eh > 0f) {
                    drawRoundRect(
                        color = if (index == selected) ExpenseColorSoft else ExpenseColor,
                        topLeft = Offset(cx + 1f, chartH - eh),
                        size = Size(barW, eh),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
                labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    bucket.label,
                    cx,
                    size.height - with(density) { 5.dp.toPx() },
                    labelPaint,
                )
            }
        }

        selected?.let { index ->
            if (index in buckets.indices) {
                val b = buckets[index]
                val pillW = with(density) { 150.dp.toPx() }
                val cx = index * slotW + slotW / 2
                val x = (cx - pillW / 2).coerceIn(0f, widthPx - pillW)
                ChartTooltip(
                    text = "${b.label}  ↑ $symbol${MoneyFormatter.format(b.income)}  ↓ $symbol${MoneyFormatter.format(b.expense)}",
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 2 — EXPENSE DONUT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExpenseDonutChart(
    stats: List<CategoryStat>,
    symbol: String,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(stats) { rotation.snapTo(0f); rotation.animateTo(1f, tween(800)) }
    val density = LocalDensity.current
    val total = stats.sumOf { it.total }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stats) {
                    detectTapGestures { offset ->
                        if (total <= 0 || stats.isEmpty()) return@detectTapGestures
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val stroke = size.minDimension * 0.22f
                        if (dist < stroke / 2 - 2 || dist > size.minDimension / 2) {
                            onSelect(null)
                            return@detectTapGestures
                        }
                        var angle = atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
                        if (angle < 0) angle += 360f
                        var sweepSum = -90f
                        var hit: Int? = null
                        stats.forEachIndexed { i, stat ->
                            val sweep = stat.total.toFloat() / total * 360f
                            val start = sweepSum
                            val end = sweepSum + sweep
                            if (angle >= start && angle < end) hit = i
                            sweepSum = end
                        }
                        onSelect(hit)
                    }
                },
        ) {
            if (stats.isEmpty()) return@Canvas
            val stroke = size.minDimension * 0.22f
            val hole = stroke / 2
            val rect = Rect(
                hole, hole,
                size.width - hole, size.height - hole,
            )
            var start = -90f
            stats.forEachIndexed { index, stat ->
                val sweep = stat.total.toFloat() / total * 360f * rotation.value
                val color = Color(stat.category?.color ?: 0xFF9E9E9E)
                val isSel = index == selectedIndex
                drawArc(
                    color = if (isSel) color.copy(alpha = 1f) else color.copy(alpha = 0.85f),
                    startAngle = start,
                    sweepAngle = sweep.coerceAtLeast(0.3f),
                    useCenter = false,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(width = stroke + if (isSel) with(density) { 6.dp.toPx() } else 0f, cap = StrokeCap.Butt),
                )
                start += stat.total.toFloat() / total * 360f
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$symbol${MoneyFormatter.format(total, 0)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 3 — DAILY SPENDING LINE (this month vs last month)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DailySpendingLineChart(
    current: List<Long>,
    previous: List<Long>,
    labels: List<String>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(current, previous) { progress.snapTo(0f); progress.animateTo(1f, tween(900)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val maxVal = maxOf(
        current.maxOrNull() ?: 0L,
        previous.maxOrNull() ?: 0L,
        1L,
    )

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(200.dp)) { widthPx, heightPx ->
        val labelH = with(density) { 22.dp.toPx() }
        val topPad = with(density) { 10.dp.toPx() }
        val chartH = heightPx - labelH - topPad
        val n = current.size
        val stepX = if (n > 1) widthPx / (n - 1) else widthPx

        fun pointFor(index: Int, value: Long): Offset {
            val x = index * stepX
            val y = chartH - (if (maxVal > 0) value.toFloat() / maxVal * chartH else 0f)
            return Offset(x, y)
        }

        val labelPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 11f
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(current, previous) {
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
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1f,
            )

            // Previous month: gray dashed
            val prevPath = Path()
            previous.forEachIndexed { i, v ->
                val p = pointFor(i, v)
                if (i == 0) prevPath.moveTo(p.x, p.y) else prevPath.lineTo(p.x, p.y)
            }
            drawPath(
                path = prevPath,
                color = Color.Gray.copy(alpha = 0.5f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
            )

            // Current month: purple solid with gradient area fill
            val curPath = Path()
            current.forEachIndexed { i, v ->
                val p = pointFor(i, v)
                if (i == 0) curPath.moveTo(p.x, p.y) else curPath.lineTo(p.x, p.y)
            }
            // draw only the visible part (progress)
            val visiblePath = Path()
            val visibleCount = (progress.value * n).toInt().coerceIn(1, n)
            (0 until visibleCount).forEach { i ->
                val p = pointFor(i, current[i])
                if (i == 0) visiblePath.moveTo(p.x, p.y) else visiblePath.lineTo(p.x, p.y)
            }
            val areaPath = Path().apply {
                addPath(visiblePath)
                lineTo(pointFor(visibleCount - 1, 0).x, chartH)
                lineTo(pointFor(0, 0).x, chartH)
                close()
            }
            drawPath(
                path = areaPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(TrendPurple.copy(alpha = 0.30f), Color.Transparent),
                    startY = topPad,
                    endY = chartH,
                ),
            )
            drawPath(
                path = visiblePath,
                color = TrendPurple,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            // points
            (0 until visibleCount).forEach { i ->
                drawCircle(color = TrendPurple, radius = 3.dp.toPx(), center = pointFor(i, current[i]))
            }

            // x labels (1, 5, 10, 15, 20, 25, 30)
            labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            labelPaint.textSize = with(density) { 11.dp.toPx() }
            val labelIndices = listOf(0, 4, 9, 14, 19, 24, 29).filter { it < n }
            labelIndices.forEach { i ->
                drawContext.canvas.nativeCanvas.drawText(
                    labels.getOrElse(i) { "" },
                    pointFor(i, 0).x,
                    size.height - with(density) { 4.dp.toPx() },
                    labelPaint,
                )
            }
        }

        selected?.let { index ->
            if (index in current.indices) {
                val pillW = with(density) { 160.dp.toPx() }
                val x = (index * stepX - pillW / 2).coerceIn(0f, widthPx - pillW)
                ChartTooltip(
                    text = "Day ${labels.getOrElse(index) { "" }}  " +
                        "$symbol${MoneyFormatter.format(current[index])}" +
                        if (index < previous.size && previous[index] > 0) "  (last: $symbol${MoneyFormatter.format(previous[index])})" else "",
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 4 — WEEKLY HEATMAP
// ─────────────────────────────────────────────────────────────────────────────

private val heatColors = listOf(
    Color(0xFFE0E0E0),
    Color(0xFFD1C4E9),
    Color(0xFFB39DDB),
    Color(0xFF7E57C2),
    Color(0xFF6A1B9A),
    Color(0xFFB71C1C),
)

@Composable
fun SpendingHeatmap(
    cells: List<HeatCell>,
    monthLabel: String,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var selected by remember { mutableStateOf<HeatCell?>(null) }
    // lay out cells Mon–Sun, starting from the first Monday at/before the first day
    val first = cells.firstOrNull()?.date
    val startOffset = first?.dayOfWeek?.value?.minus(1) ?: 0
    val weeks = (cells.size + startOffset + 6) / 7
    val cellSize = with(density) { 20.dp.toPx() }
    val gap = with(density) { 5.dp.toPx() }

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(160.dp)) { widthPx, heightPx ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cells) {
                    detectTapGestures { offset ->
                        if (cells.isEmpty()) return@detectTapGestures
                        val col = (offset.x / (cellSize + gap)).toInt().coerceIn(0, 6)
                        val row = (offset.y / (cellSize + gap)).toInt().coerceIn(0, weeks - 1)
                        val index = row * 7 + col - startOffset
                        if (index in cells.indices) {
                            selected = if (selected?.date == cells[index].date) null else cells[index]
                        }
                    }
                },
        ) {
            cells.forEachIndexed { index, cell ->
                val row = (index + startOffset) / 7
                val col = (index + startOffset) % 7
                val x = col * (cellSize + gap)
                val y = row * (cellSize + gap)
                drawRoundRect(
                    color = if (cell.level == 0) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        heatColors[cell.level]
                    },
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(with(density) { 4.dp.toPx() }),
                )
            }
        }

        selected?.let { cell ->
            val idx = cells.indexOf(cell)
            if (idx >= 0) {
                val col = (idx + startOffset) % 7
                val x = col * (cellSize + gap)
                val pillW = with(density) { 150.dp.toPx() }
                val px = (x - pillW / 2 + cellSize / 2).coerceIn(0f, (widthPx - pillW).coerceAtLeast(0f))
                ChartTooltip(
                    text = "${cell.date}  $symbol${MoneyFormatter.format(cell.expense)}",
                    modifier = Modifier.offset { IntOffset(px.roundToInt(), 0) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 5 — CATEGORY TREND MULTI-LINE
// ─────────────────────────────────────────────────────────────────────────────

private val trendPalette = listOf(
    Color(0xFF6C63FF),
    Color(0xFF3B82F6),
    Color(0xFFEC407A),
    Color(0xFFFF9800),
    Color(0xFF26A69A),
)

@Composable
fun CategoryTrendChart(
    trends: List<CategoryTrend>,
    months: List<String>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trends) { progress.snapTo(0f); progress.animateTo(1f, tween(1000)) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (trendIdx, monthIdx)
    val density = LocalDensity.current
    val maxVal = maxOf(trends.maxOfOrNull { t -> t.values.maxOrNull() ?: 0L } ?: 1L, 1L)
    val n = months.size

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(200.dp)) { widthPx, heightPx ->
        val labelH = with(density) { 22.dp.toPx() }
        val chartH = heightPx - labelH - with(density) { 8.dp.toPx() }
        val stepX = if (n > 1) widthPx / (n - 1) else widthPx

        fun yFor(v: Long) = chartH - (if (maxVal > 0) v.toFloat() / maxVal * chartH else 0f)

        val labelPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 11f
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trends) {
                    detectTapGestures { offset ->
                        if (n == 0) return@detectTapGestures
                        val monthIdx = (offset.x / (widthPx / n)).toInt().coerceIn(0, n - 1)
                        var best: Pair<Int, Int>? = null
                        var bestDist = Float.MAX_VALUE
                        trends.forEachIndexed { ti, trend ->
                            val p = Offset(monthIdx * stepX, yFor(trend.values.getOrElse(monthIdx) { 0L }))
                            val dist = abs(p.x - offset.x) + abs(p.y - offset.y)
                            if (dist < bestDist) { bestDist = dist; best = ti to monthIdx }
                        }
                        selected = if (selected == best) null else best
                    }
                },
        ) {
            if (n == 0) return@Canvas
            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1f,
            )

            trends.forEachIndexed { ti, trend ->
                val color = trendPalette[ti % trendPalette.size]
                val path = Path()
                val visibleCount = (progress.value * n).toInt().coerceIn(1, n)
                (0 until visibleCount).forEach { mi ->
                    val p = Offset(mi * stepX, yFor(trend.values.getOrElse(mi) { 0L }))
                    if (mi == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                (0 until visibleCount).forEach { mi ->
                    val p = Offset(mi * stepX, yFor(trend.values.getOrElse(mi) { 0L }))
                    drawCircle(
                        color = if (selected?.first == ti && selected?.second == mi) color else color.copy(alpha = 0.6f),
                        radius = if (selected?.first == ti && selected?.second == mi) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = p,
                    )
                }
            }

            labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            labelPaint.textSize = with(density) { 11.dp.toPx() }
            months.forEachIndexed { mi, label ->
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    mi * stepX,
                    size.height - with(density) { 4.dp.toPx() },
                    labelPaint,
                )
            }
        }

        selected?.let { (ti, mi) ->
            if (ti in trends.indices && mi in months.indices) {
                val trend = trends[ti]
                val pillW = with(density) { 170.dp.toPx() }
                val x = (mi * stepX - pillW / 2).coerceIn(0f, widthPx - pillW)
                ChartTooltip(
                    text = "${trend.name} · ${months[mi]}  $symbol${MoneyFormatter.format(trend.values.getOrElse(mi) { 0L })}",
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 6 — INCOME SOURCES PIE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomeSourcePie(
    stats: List<CategoryStat>,
    symbol: String,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(stats) { rotation.snapTo(0f); rotation.animateTo(1f, tween(800)) }
    val density = LocalDensity.current
    val total = stats.sumOf { it.total }
    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .pointerInput(stats) {
                detectTapGestures { offset ->
                    if (total <= 0) return@detectTapGestures
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > size.minDimension / 2) { onSelect(null); return@detectTapGestures }
                    var angle = atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
                    if (angle < 0) angle += 360f
                    var sweepSum = -90f
                    var hit: Int? = null
                    stats.forEachIndexed { i, stat ->
                        val sweep = stat.total.toFloat() / total * 360f
                        if (angle >= sweepSum && angle < sweepSum + sweep) hit = i
                        sweepSum += sweep
                    }
                    onSelect(hit)
                }
            },
    ) {
        if (stats.isEmpty()) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        var start = -90f
        stats.forEachIndexed { index, stat ->
            val sweep = stat.total.toFloat() / total * 360f * rotation.value
            val color = Color(stat.category?.color ?: 0xFF9E9E9E)
            val isSel = index == selectedIndex
            val pull = if (isSel) radius * 0.06f else 0f
            val mid = start + sweep / 2
            val midRad = mid * kotlin.math.PI.toFloat() / 180f
            val pullOffset = Offset(cos(midRad) * pull, sin(midRad) * pull)
            drawArc(
                color = if (isSel) color.copy(alpha = 1f) else color.copy(alpha = 0.85f),
                startAngle = start,
                sweepAngle = sweep.coerceAtLeast(0.5f),
                useCenter = true,
                topLeft = Offset(center.x - radius + pullOffset.x, center.y - radius + pullOffset.y),
                size = Size(radius * 2, radius * 2),
            )
            // label outside the slice
            if (sweep >= 18f) {
                val labelRad = (mid) * kotlin.math.PI.toFloat() / 180f
                val labelR = radius + with(density) { 16.dp.toPx() }
                val lx = center.x + cos(labelRad) * labelR
                val ly = center.y + sin(labelRad) * labelR
                labelPaint.textSize = with(density) { 10.dp.toPx() }
                labelPaint.color = MaterialTheme.colorScheme.onSurface.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    "${(stat.fraction * 100).toInt()}%",
                    lx,
                    ly,
                    labelPaint,
                )
            }
            start += stat.total.toFloat() / total * 360f
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 7 — SAVINGS RATE GAUGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SavingsRateGauge(
    rate: Float,
    income: Long,
    expense: Long,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val needle = remember { Animatable(0f) }
    LaunchedEffect(rate) { needle.snapTo(0f); needle.animateTo(rate, tween(1100)) }
    val density = LocalDensity.current
    val percent = (rate * 100).roundToInt()

    Box(modifier = modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            val center = Offset(size.width / 2f, size.height - with(density) { 16.dp.toPx() })
            val radius = min(size.width / 2f - with(density) { 8.dp.toPx() }, size.height - with(density) { 24.dp.toPx() })
            val stroke = with(density) { 16.dp.toPx() }

            // zones: red 0-30, orange 30-60, yellow 60-80, green 80-100
            val zones = listOf(
                0f to 0.30f to Color(0xFFE53935),
                0.30f to 0.60f to Color(0xFFFF9800),
                0.60f to 0.80f to Color(0xFFFDD835),
                0.80f to 1.01f to Color(0xFF4CAF50),
            )
            zones.forEach { (range, color) ->
                val (start, end) = range
                drawArc(
                    color = color.copy(alpha = 0.85f),
                    startAngle = 180f - 180f * end,
                    sweepAngle = 180f * (end - start),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            // needle
            val needleAngle = 180f - 180f * needle.value
            val needleRad = needleAngle * kotlin.math.PI.toFloat() / 180f
            val nx = center.x + cos(needleRad) * (radius - with(density) { 18.dp.toPx() })
            val ny = center.y + sin(needleRad) * (radius - with(density) { 18.dp.toPx() })
            drawLine(
                color = Color(0xFF37474F),
                start = center,
                end = Offset(nx, ny),
                strokeWidth = with(density) { 4.dp.toPx() },
                cap = StrokeCap.Round,
            )
            drawCircle(color = Color(0xFF37474F), radius = with(density) { 7.dp.toPx() }, center = center)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-18).dp),
        ) {
            Text(
                text = "$percent% saved this month",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Saved $symbol${MoneyFormatter.format(income - expense, 0)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 8 — NET WORTH AREA CHART
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NetWorthChart(
    points: List<NetWorthPoint>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) { progress.snapTo(0f); progress.animateTo(1f, tween(900)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val minVal = points.minOfOrNull { it.value } ?: 0L
    val maxVal = points.maxOfOrNull { it.value } ?: 0L
    val range = (maxVal - minVal).coerceAtLeast(1L)
    val n = points.size

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(200.dp)) { widthPx, heightPx ->
        val labelH = with(density) { 22.dp.toPx() }
        val chartH = heightPx - labelH - with(density) { 8.dp.toPx() }
        val stepX = if (n > 1) widthPx / (n - 1) else widthPx

        fun yFor(v: Long) = chartH - (v - minVal).toFloat() / range * chartH
        fun xFor(i: Int) = i * stepX

        val labelPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 11f
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
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
                start = Offset(0f, chartH),
                end = Offset(size.width, chartH),
                strokeWidth = 1f,
            )
            val visibleCount = (progress.value * n).toInt().coerceIn(1, n)
            val linePath = Path()
            (0 until visibleCount).forEach { i ->
                val p = Offset(xFor(i), yFor(points[i].value))
                if (i == 0) linePath.moveTo(p.x, p.y) else linePath.lineTo(p.x, p.y)
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(xFor(visibleCount - 1), chartH)
                lineTo(xFor(0), chartH)
                close()
            }
            drawPath(
                path = areaPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(IncomeColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = chartH,
                ),
            )
            drawPath(
                path = linePath,
                color = IncomeColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            (0 until visibleCount).forEach { i ->
                drawCircle(
                    color = if (selected == i) IncomeColorBright else IncomeColor.copy(alpha = 0.7f),
                    radius = if (selected == i) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(xFor(i), yFor(points[i].value)),
                )
            }
            labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            labelPaint.textSize = with(density) { 11.dp.toPx() }
            points.forEachIndexed { i, p ->
                if (i % 2 == 0 || i == n - 1) {
                    drawContext.canvas.nativeCanvas.drawText(
                        p.label,
                        xFor(i),
                        size.height - with(density) { 4.dp.toPx() },
                        labelPaint,
                    )
                }
            }
        }

        selected?.let { index ->
            if (index in points.indices) {
                val pillW = with(density) { 160.dp.toPx() }
                val x = (xFor(index) - pillW / 2).coerceIn(0f, widthPx - pillW)
                ChartTooltip(
                    text = "${points[index].label}  Net Worth: $symbol${MoneyFormatter.format(points[index].value, 0)}",
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 0) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 9 — TOP SPENDING HORIZONTAL BARS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TopSpendingBars(
    items: List<CategoryStat>,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val maxVal = items.maxOfOrNull { it.total } ?: 1L
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, stat ->
            val color = Color(stat.category?.color ?: 0xFF6C63FF)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(18.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .padding(end = 4.dp),
                ) {
                    // track
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 0.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {}
                    }
                    // fill (static width by fraction; animated height not needed here)
                    val fraction = if (maxVal > 0) stat.total.toFloat() / maxVal else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                            .fillMaxSize(),
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 1.dp),
                            shape = RoundedCornerShape(50),
                            color = color,
                        ) {}
                    }
                }
                Text(
                    text = stat.category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.width(96.dp),
                )
                Text(
                    text = "$symbol${MoneyFormatter.format(stat.total, 0)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHART 10 — CASH FLOW WATERFALL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CashFlowWaterfall(
    flow: CashFlow,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(flow) { progress.snapTo(0f); progress.animateTo(1f, tween(800)) }
    val density = LocalDensity.current

    // bars: Opening (gray-blue), +Income (green), -Expense (red), Closing (blue)
    data class WaterBar(val label: String, val start: Long, val end: Long, val color: Color, val positive: Boolean)
    val bars = listOf(
        WaterBar("Opening", 0, flow.opening, Color(0xFF78909C), flow.opening >= 0),
        WaterBar("Income", flow.opening, flow.opening + flow.income, IncomeColor, true),
        WaterBar("Expenses", flow.opening + flow.income, flow.opening + flow.income - flow.expense, ExpenseColor, false),
        WaterBar("Closing", 0, flow.closing, Color(0xFF3B82F6), flow.closing >= 0),
    )
    val allVals = listOf(flow.opening, flow.opening + flow.income, flow.opening + flow.income - flow.expense, flow.closing)
    val minVal = minOf(0L, allVals.minOrNull() ?: 0L)
    val maxVal = maxOf(0L, allVals.maxOrNull() ?: 0L)
    val range = (maxVal - minVal).coerceAtLeast(1L)

    BoxWithConstraintsCompat(modifier = modifier.fillMaxWidth().height(200.dp)) { widthPx, heightPx ->
        val labelH = with(density) { 22.dp.toPx() }
        val chartH = heightPx - labelH - with(density) { 8.dp.toPx() }
        val slotW = widthPx / bars.size
        val barW = minOf(slotW * 0.5f, with(density) { 40.dp.toPx() })

        fun yFor(v: Long) = chartH - (v - minVal).toFloat() / range * chartH

        val labelPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 11f
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, yFor(0)),
                end = Offset(size.width, yFor(0)),
                strokeWidth = 1f,
            )
            // connectors
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            (0 until bars.size - 1).forEach { i ->
                val fromX = i * slotW + slotW / 2
                val toX = (i + 1) * slotW + slotW / 2
                drawLine(
                    color = MaterialTheme.colorScheme.outline,
                    start = Offset(fromX, yFor(bars[i].end)),
                    end = Offset(toX, yFor(bars[i].end)),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dash,
                )
            }
            bars.forEachIndexed { i, bar ->
                val cx = i * slotW + slotW / 2
                val topY = yFor(maxOf(bar.start, bar.end))
                val bottomY = yFor(minOf(bar.start, bar.end))
                val h = (bottomY - topY) * progress.value
                drawRoundRect(
                    color = bar.color,
                    topLeft = Offset(cx - barW / 2, bottomY - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2),
                )
                labelPaint.color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                labelPaint.textSize = with(density) { 10.dp.toPx() }
                drawContext.canvas.nativeCanvas.drawText(
                    bar.label,
                    cx,
                    size.height - with(density) { 4.dp.toPx() },
                    labelPaint,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helper: BoxWithConstraints without the modifier-based lambda friction
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxWithConstraintsCompat(
    modifier: Modifier,
    content: @androidx.compose.foundation.layout.BoxWithConstraintsScope.(widthPx: Float, heightPx: Float) -> Unit,
) {
    val density = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier,
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        content(w, h)
    }
}
