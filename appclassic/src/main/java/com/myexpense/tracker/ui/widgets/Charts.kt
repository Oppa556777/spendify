package com.myexpense.tracker.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.myexpense.tracker.db.CategoryStat
import com.myexpense.tracker.util.Format

/** Donut chart for the category breakdown. */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var stats: List<CategoryStat> = emptyList()
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var total = 0L
    private var centerText = ""
    private var subText = ""

    fun setStats(list: List<CategoryStat>) {
        stats = list
        total = list.sumOf { it.total }
        centerText = Format.money(total, "")
        subText = "spent"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val stroke = w * 0.16f
        arcPaint.strokeWidth = stroke
        val rect = RectF(stroke / 2, stroke / 2, w - stroke / 2, h - stroke / 2)

        if (total > 0 && stats.isNotEmpty()) {
            var start = -90f
            stats.forEach { stat ->
                val sweep = stat.total.toFloat() / total * 360f
                arcPaint.color = stat.categoryColor
                canvas.drawArc(rect, start, (sweep - 2f).coerceAtLeast(0.5f), false, arcPaint)
                start += sweep
            }
        } else {
            arcPaint.color = 0xFFE0E0E0.toInt()
            canvas.drawArc(rect, 0f, 360f, false, arcPaint)
        }

        textPaint.textSize = w * 0.16f
        textPaint.color = if (isDark()) Color.WHITE else Color.BLACK
        canvas.drawText(centerText, w / 2, h / 2 + 6, textPaint)
        labelPaint.textSize = w * 0.07f
        labelPaint.color = if (isDark()) Color.GRAY else 0xFF616161.toInt()
        canvas.drawText(subText, w / 2, h / 2 + 6 + textPaint.textSize, labelPaint)
    }

    private fun isDark(): Boolean {
        val mode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}

/** Grouped column chart: income + expense per month (12 months). */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var series: List<Pair<String, Pair<Long, Long>>> = emptyList()
    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2E7D32.toInt() }
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC62828.toInt() }
    private val gridPaint = Paint().apply { color = 0xFFBDBDBD.toInt(); strokeWidth = 1f }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    fun setSeries(data: List<Pair<String, Pair<Long, Long>>>) {
        series = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = 40f
        val chartH = h - labelH - 12f
        val maxVal = series.maxOfOrNull { maxOf(it.second.first, it.second.second) } ?: 1L
        val n = series.size
        if (n == 0) return

        // grid line
        gridPaint.color = if (isDark()) 0xFF424242.toInt() else 0xFFE0E0E0.toInt()
        canvas.drawLine(0f, chartH, w, chartH, gridPaint)

        val groupW = w / n
        val barW = (groupW * 0.32f).coerceAtMost(28f)
        labelPaint.color = if (isDark()) Color.WHITE else 0xFF424242.toInt()

        series.forEachIndexed { i, (month, pair) ->
            val cx = i * groupW + groupW / 2
            val (income, expense) = pair
            val ih = if (maxVal > 0) income.toFloat() / maxVal * chartH else 0f
            val eh = if (maxVal > 0) expense.toFloat() / maxVal * chartH else 0f
            canvas.drawRect(cx - barW - 1f, chartH - ih, cx - 1f, chartH, incomePaint)
            canvas.drawRect(cx + 1f, chartH - eh, cx + barW + 1f, chartH, expensePaint)
            canvas.drawText(Format.shortMonth(month), cx, h - 8f, labelPaint)
        }
    }

    private fun isDark(): Boolean {
        val mode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
