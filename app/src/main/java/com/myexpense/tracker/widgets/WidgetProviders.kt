package 
import kotlinx.coroutines.flow.first
com.myexpense.tracker.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.myexpense.tracker.MainActivity
import com.myexpense.tracker.R
import com.myexpense.tracker.data.database.AppDatabase
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.utils.MoneyFormatter
import com.myexpense.tracker.utils.toEpochMillis
import com.myexpense.tracker.utils.toLocalDate
import com.myexpense.tracker.utils.toMinorUnits
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

private const val EXTRA_DESTINATION = "open_destination"

/** Widget tap opens the app (optionally a destination screen). */
private fun openAppPendingIntent(context: Context, destination: String? = null): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        destination?.let { putExtra(EXTRA_DESTINATION, it) }
    }
    return PendingIntent.getActivity(context, destination.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

/** Widget 1 — Balance widget (2×1): today's spending + total balance. */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val (totalBalance, todaySpending, symbol) = runBlocking {
            val db = AppDatabase.getWidgetInstance(context)
            val symbol = SettingsRepository(context).currentSymbol()
            val total = db.accountDao().observeAll().first().sumOf { it.balance.toMinorUnits() }
            val today = LocalDate.now()
            val spent = db.transactionDao().expenseBetween(today.toEpochMillis(), today.toEpochMillis()).toMinorUnits()
            Triple(total, spent, symbol)
        }

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_balance).apply {
                setTextViewText(R.id.widget_total_balance, "${symbol}${MoneyFormatter.format(totalBalance)}")
                setTextViewText(R.id.widget_today_spending, "Today's spending: ${symbol}${MoneyFormatter.format(todaySpending)}")
                setOnClickPendingIntent(R.id.widget_balance_root, openAppPendingIntent(context))
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

/** Widget 2 — Spending graph widget (4×2): 7-day bar chart + today's total. */
class GraphWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val (daily, todayTotal, symbol) = runBlocking {
            val db = AppDatabase.getWidgetInstance(context)
            val symbol = SettingsRepository(context).currentSymbol()
            val today = LocalDate.now()
            val from = today.minusDays(6)
            val rows = db.transactionDao().observeDailyTotals(from.toEpochMillis(), today.toEpochMillis()).first()
            val byDay = rows.associate { it.day to it.total.toMinorUnits() }
            val values = (0..6).map { off ->
                byDay[from.plusDays(off.toLong()).toString()] ?: 0L
            }
            val todayVal = values.last()
            Triple(values, todayVal, symbol)
        }

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_graph).apply {
                setTextViewText(R.id.widget_graph_today, "Today: ${symbol}${MoneyFormatter.format(todayTotal)}")
                setOnClickPendingIntent(R.id.widget_graph_root, openAppPendingIntent(context, "reports"))
            }
            val max = daily.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            daily.forEachIndexed { index, value ->
                val heightPx = (value.toFloat() / max * 80).toInt().coerceIn(4, 80)
                views.setViewLayoutParams(
                    barId(index),
                    RemoteViews.LayoutParams(RemoteViews.LayoutParams.MATCH_PARENT, heightPx),
                )
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun barId(index: Int): Int = when (index) {
        0 -> R.id.bar0
        1 -> R.id.bar1
        2 -> R.id.bar2
        3 -> R.id.bar3
        4 -> R.id.bar4
        5 -> R.id.bar5
        else -> R.id.bar6
    }
}

/** Tiny synchronous DB accessor used by the widget providers. */
private fun AppDatabase.Companion.getWidgetInstance(context: Context): AppDatabase =
    androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "moneymate.db")
        .allowMainThreadQueries()
        .build()
