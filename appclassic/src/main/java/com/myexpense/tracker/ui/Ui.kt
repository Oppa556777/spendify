package com.myexpense.tracker.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs

/** Applies the user's theme choice. Call before setContentView. */
fun Activity.applyThemeChoice() {
    when (Prefs.themeMode(this)) {
        Prefs.THEME_LIGHT -> setTheme(R.style.Theme_MoneyMate_Light)
        Prefs.THEME_DARK -> setTheme(R.style.Theme_MoneyMate_Dark)
    }
}

fun Context.dp(v: Int): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
).toInt()

fun Context.dpf(v: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics
)

fun Context.color(id: Int): Int = resources.getColor(id, null)

fun Context.font(res: Int): Typeface = resources.getFont(res)

/** Rounded circle with a centered emoji. */
class CircleBadge(context: Context) : View(context) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    var emoji: String = "📦"
        set(value) { field = value; invalidate() }
    var badgeColor: Int = 0xFF4CAF50.toInt()
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = minOf(width, height) / 2f
        bg.color = badgeColor
        bg.alpha = 40
        canvas.drawCircle(width / 2f, height / 2f, r, bg)
        emojiPaint.textSize = r * 1.1f
        val y = height / 2f - (emojiPaint.descent() + emojiPaint.ascent()) / 2f
        canvas.drawText(emoji, width / 2f, y, emojiPaint)
    }
}

/** ‹ June 2026 › month navigator. */
fun monthNav(
    context: Context,
    month: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
): LinearLayout {
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    val prev = ImageButton(context).apply {
        setBackgroundResource(android.R.drawable.ic_media_previous)
        setOnClickListener { onPrev() }
    }
    val next = ImageButton(context).apply {
        setBackgroundResource(android.R.drawable.ic_media_next)
        setOnClickListener { onNext() }
    }
    val title = TextView(context).apply {
        text = Format.monthYear(month)
        textSize = 17f
        typeface = Typeface.create(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setTextColor(context.color(R.color.text))
    }
    row.addView(prev, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
    row.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    row.addView(next, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
    return row
}

fun sectionLabel(context: Context, text: String): TextView =
    TextView(context).apply {
        this.text = text.uppercase()
        textSize = 12f
        typeface = Typeface.create(typeface, Typeface.BOLD)
        setTextColor(context.color(R.color.primary))
        setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(6))
    }

fun LinearLayout.addRow(
    context: Context,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
): LinearLayout {
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
        isClickable = onClick != null
        isFocusable = onClick != null
        if (onClick != null) setOnClickListener { onClick() }
    }
    val col = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    col.addView(TextView(context).apply {
        text = title
        textSize = 15f
        setTextColor(context.color(R.color.text))
    })
    if (subtitle != null) {
        col.addView(TextView(context).apply {
            text = subtitle
            textSize = 12f
            setTextColor(context.color(R.color.subtext))
        })
    }
    row.addView(col)
    addView(row)
    return row
}

fun divider(context: Context): View = View(context).apply {
    setBackgroundColor(context.color(R.color.divider))
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
}

fun progressBar(context: Context, progress: Float, color: Int): ProgressBar =
    ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        this.progress = (progress * 1000).toInt().coerceIn(0, 1000)
        progressTintList = android.content.res.ColorStateList.valueOf(color)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.divider))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(10))
    }

fun confirm(
    activity: Activity,
    title: String,
    message: String,
    positive: String = "Delete",
    onYes: () -> Unit,
) {
    AlertDialog.Builder(activity)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positive) { _, _ -> onYes() }
        .setNegativeButton("Cancel", null)
        .show()
}
