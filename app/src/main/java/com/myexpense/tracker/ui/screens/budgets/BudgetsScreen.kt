package com.myexpense.tracker.ui.screens.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetPeriod
import com.myexpense.tracker.data.model.BudgetWithSpent
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.MonthSelector
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.viewmodel.BudgetsViewModel
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    var editing by remember { mutableStateOf<BudgetWithSpent?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add budget")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )

            val totalBudget = state.budgets.sumOf { it.budget.limitAmount }
            val totalSpent = state.budgets.sumOf { it.spent }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total budget: ${MoneyFormatter.formatWithSymbol(totalBudget, state.currencySymbol)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Spent: ${MoneyFormatter.formatWithSymbol(totalSpent, state.currencySymbol)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.budgets.isEmpty()) {
                EmptyState(
                    title = "No budgets for ${state.month.toString()}",
                    subtitle = "Create a budget per category to stay on track.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.budgets, key = { it.budget.id }) { budgetWithSpent ->
                        BudgetCard(
                            budgetWithSpent = budgetWithSpent,
                            symbol = state.currencySymbol,
                            onClick = {
                                editing = budgetWithSpent
                                showDialog = true
                            },
                            onDelete = { viewModel.delete(budgetWithSpent.budget) },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            existing = editing?.budget,
            categories = state.categories,
            symbol = state.currencySymbol,
            onDismiss = { showDialog = false },
            onSave = { budget ->
                viewModel.save(budget)
                showDialog = false
            },
        )
    }
}

@Composable
private fun BudgetCard(
    budgetWithSpent: BudgetWithSpent,
    symbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val budget = budgetWithSpent.budget
    val category = budgetWithSpent.category
    val color = Color(category?.color ?: 0xFF4CAF50)
    val overspent = budgetWithSpent.isOverspent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(icon = category?.icon, color = color, size = 44)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = category?.name ?: "All categories",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = budget.period.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { budgetWithSpent.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (overspent) MaterialTheme.colorScheme.error else color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${MoneyFormatter.format(budgetWithSpent.spent)} / ${MoneyFormatter.format(budget.limitAmount)} $symbol" +
                        if (overspent) "  •  Overspent by ${MoneyFormatter.format(-budgetWithSpent.remaining)}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetDialog(
    existing: Budget?,
    categories: List<com.myexpense.tracker.data.model.Category>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit,
) {
    var categoryId by remember {
        mutableStateOf(existing?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var amount by remember { mutableStateOf(existing?.let { MoneyFormatter.format(it.limitAmount) } ?: "") }
    var period by remember { mutableStateOf(existing?.period ?: BudgetPeriod.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isNotEmpty()) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { category ->
                            val selected = categoryId == category.id
                            Surface(
                                modifier = Modifier.clickable { categoryId = category.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    Color(category.color).copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Limit amount") },
                    prefix = { Text(symbol) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Period", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BudgetPeriod.entries.forEach { p ->
                        val selected = period == p
                        Surface(
                            modifier = Modifier.clickable { period = p },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ) {
                            Text(
                                p.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = amount.toDoubleOrNull() ?: 0.0
                if (parsed > 0 && categoryId != 0L) {
                    onSave(
                        Budget(
                            id = existing?.id ?: 0,
                            name = existing?.name ?: "",
                            categoryId = categoryId,
                            limitAmount = (parsed * 100).toLong().coerceAtLeast(1),
                            spentAmount = existing?.spentAmount ?: 0,
                            period = period,
                            startDate = existing?.startDate ?: System.currentTimeMillis(),
                            endDate = existing?.endDate,
                            colorHex = existing?.colorHex ?: "#4CAF50",
                            alertAt = existing?.alertAt ?: 80,
                            isActive = existing?.isActive ?: true,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
