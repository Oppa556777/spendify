package com.myexpense.tracker.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.DailyStat
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.CalendarViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    val weekStartIso = 1 // Monday-based grid (dayOfWeek.value)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Month nav + totals ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = DateUtils.monthYear(state.month),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "In: $symbol${MoneyFormatter.format(state.monthIncome)}  ·  Out: $symbol${MoneyFormatter.format(state.monthExpense)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
            }

            // ── Calendar grid ───────────────────────────────────────────────
            item {
                CalendarGrid(
                    month = state.month,
                    days = state.days,
                    selected = state.selectedDate,
                    weekStartIso = weekStartIso,
                    onSelect = viewModel::select,
                )
            }

            // ── Selected day transactions ───────────────────────────────────
            state.selectedDate?.let { selected ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = DateUtils.fullDate(selected),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::clearSelection) {
                            Text("Clear")
                        }
                    }
                }
                if (state.dayTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No transactions",
                            subtitle = "Nothing was recorded on this day.",
                        )
                    }
                } else {
                    items(state.dayTransactions, key = { it.id }) { t ->
                        TransactionRow(
                            title = t.title.ifBlank { if (t.isExpense) "Expense" else "Income" },
                            subtitle = if (t.time.isNotBlank()) t.time else "",
                            icon = null,
                            iconColor = if (t.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            amount = t.amount,
                            symbol = symbol,
                            isExpense = t.isExpense,
                            onClick = { onEditTransaction(t.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    days: List<DailyStat>,
    selected: LocalDate?,
    weekStartIso: Int,
    onSelect: (LocalDate) -> Unit,
) {
    val dayMap = days.associateBy { it.date }
    val maxExpense = days.maxOfOrNull { it.expense } ?: 1L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    val dow = ((weekStartIso - 1 + offset) % 7) + 1
                    Text(
                        text = LocalDate.of(2026, 1, 1).plusDays((dow - 1).toLong())
                            .dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Leading blanks so day 1 lands on the right weekday
            val firstDay = month.atDay(1)
            val lead = (firstDay.dayOfWeek.value - weekStartIso + 7) % 7
            val daysInMonth = month.lengthOfMonth()
            val cells = lead + daysInMonth
            val weeks = (cells + 6) / 7

            repeat(weeks) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    (0..6).forEach { col ->
                        val cellIndex = week * 7 + col
                        val dayNumber = cellIndex - lead + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val date = month.atDay(dayNumber)
                                val stat = dayMap[date]
                                val expense = stat?.expense ?: 0L
                                val isToday = date == LocalDate.now()
                                val isSelected = date == selected
                                val intensity = if (maxExpense > 0) {
                                    (expense.toFloat() / maxExpense).coerceIn(0f, 1f)
                                } else 0f

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelect(date) }
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .padding(vertical = 3.dp),
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    // spending intensity dot
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    expense <= 0 -> Color.Transparent
                                                    intensity < 0.33f -> Color(0xFFB39DDB)
                                                    intensity < 0.66f -> Color(0xFF7E57C2)
                                                    else -> Color(0xFFB71C1C)
                                                }
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { content() }
}
