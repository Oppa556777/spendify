package com.myexpense.tracker.ui.screens.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.BudgetStatus
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.BudgetFilter
import com.myexpense.tracker.ui.viewmodel.BudgetsViewModel
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    onBudgetClick: (Long) -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BudgetWithSpent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Budgets", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add budget", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Summary header ──────────────────────────────────────────────
            item {
                BudgetSummaryHeader(
                    budgeted = state.totalBudgeted,
                    spent = state.totalSpent,
                    remaining = state.remaining,
                    progress = state.overallProgress,
                    symbol = symbol,
                )
            }

            // ── Status filter tabs ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BudgetFilter.entries.forEach { f ->
                        val selected = state.filter == f
                        Surface(
                            modifier = Modifier.clickable { viewModel.setFilter(f) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                            border = if (!selected) {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            } else null,
                        ) {
                            Text(
                                text = f.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            if (state.budgets.isEmpty()) {
                item {
                    EmptyState(
                        title = "No budgets here",
                        subtitle = when (state.filter) {
                            BudgetFilter.ALL -> "Tap + to create your first budget."
                            BudgetFilter.ACTIVE -> "No active budgets. Create one to stay on track."
                            BudgetFilter.OVER_BUDGET -> "Nothing is over budget. 🎉"
                            BudgetFilter.COMPLETED -> "No completed budgets yet."
                        },
                    )
                }
            } else {
                items(state.budgets, key = { it.budget.id }) { budgetWithSpent ->
                    BudgetCardView(
                        budgetWithSpent = budgetWithSpent,
                        symbol = symbol,
                        onClick = { onBudgetClick(budgetWithSpent.budget.id) },
                        onDelete = { pendingDelete = budgetWithSpent },
                    )
                }
            }
        }
    }

    // ── Delete confirmation ─────────────────────────────────────────────────
    pendingDelete?.let { bws ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete budget?") },
            text = { Text("Delete \"${bws.budget.name.ifBlank { bws.category?.name ?: "Budget" }}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(bws.budget)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // ── Add budget sheet ────────────────────────────────────────────────────
    if (showSheet) {
        AddEditBudgetSheet(
            budget = null,
            categories = state.categories,
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { budget ->
                viewModel.save(budget)
                showSheet = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetSummaryHeader(
    budgeted: Long,
    spent: Long,
    remaining: Long,
    progress: Float,
    symbol: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BudgetRing(progress = progress)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryLine("Total Budgeted", budgeted, symbol, Color(0xFF6C63FF))
                SummaryLine("Total Spent", spent, symbol, Color(0xFFE53935))
                SummaryLine(
                    "Remaining",
                    remaining,
                    symbol,
                    if (remaining >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                )
            }
        }
    }
}

@Composable
private fun BudgetRing(progress: Float) {
    val animated by animateFloatAsState(progress, tween(700), label = "budgetRing")
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.11f
            drawArc(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color(0xFF6C63FF),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: Long, symbol: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$symbol${MoneyFormatter.format(value)}",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BUDGET CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetCardView(
    budgetWithSpent: BudgetWithSpent,
    symbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val budget = budgetWithSpent.budget
    val category = budgetWithSpent.category
    val accent = Color(budget.colorHex.removePrefix("#").toLongOrNull(16)?.let { 0xFF000000 or it } ?: 0xFF6C63FF)
    val overspent = budgetWithSpent.isOverspent
    val completed = budgetWithSpent.status == BudgetStatus.COMPLETED

    val barColor = when {
        overspent -> Color(0xFFB71C1C)
        budgetWithSpent.progress > 0.8f -> Color(0xFFF44336)
        budgetWithSpent.progress > 0.6f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    val animated by animateFloatAsState(budgetWithSpent.progress, tween(600), label = "budgetBar")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        // Left accent border
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(accent),
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            color = if (overspent) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Row 1: icon + name | period chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(icon = category?.icon, color = accent, size = 34)
                    Text(
                        text = budget.name.ifBlank { category?.name ?: "All Expenses" },
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                    if (overspent) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = budget.period.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Row 2: spent of limit
                Text(
                    text = "$symbol${MoneyFormatter.format(budgetWithSpent.spent)} spent of $symbol${MoneyFormatter.format(budget.limitAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                // Row 3: progress bar
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth(),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(Modifier.height(6.dp))
                // Row 4: remaining + %
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (overspent) {
                        Text(
                            text = "$symbol${MoneyFormatter.format(-budgetWithSpent.remaining)} over budget!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                    } else if (completed) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = "$symbol${MoneyFormatter.format(budgetWithSpent.remaining)} remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (budgetWithSpent.remaining < 0) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "${(budgetWithSpent.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                    )
                }
            }
        }
    }
}
