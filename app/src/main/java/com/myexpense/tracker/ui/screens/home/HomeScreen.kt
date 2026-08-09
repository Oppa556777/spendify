package com.myexpense.tracker.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.HeroBalanceCard
import com.myexpense.tracker.ui.components.SectionHeader
import com.myexpense.tracker.ui.components.StatChip
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.viewmodel.HomeViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MoneyMate", style = MaterialTheme.typography.titleLarge)
                        Text(
                            DateUtils.monthYear(state.month),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
            item {
                HeroBalanceCard(
                    title = "Total Balance",
                    balanceText = MoneyFormatter.formatWithSymbol(state.totalBalance, symbol),
                    subtitle = "Across all accounts",
                    modifier = Modifier.fillMaxWidth(),
                    gradient = listOf(Color(0xFF2E7D32), Color(0xFF4CAF50)),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatChip(
                        label = "Income",
                        value = MoneyFormatter.formatWithSymbol(state.income, symbol),
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatChip(
                        label = "Expenses",
                        value = MoneyFormatter.formatWithSymbol(state.expense, symbol),
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = { onAddTransaction(TransactionType.EXPENSE) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.RemoveCircle, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text("Expense")
                    }
                    Button(
                        onClick = { onAddTransaction(TransactionType.INCOME) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text("Income")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickLinkCard(
                        label = "Accounts",
                        icon = Icons.Filled.AccountBalanceWallet,
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f),
                    )
                    QuickLinkCard(
                        label = "Categories",
                        icon = Icons.Filled.Category,
                        onClick = onNavigateToCategories,
                        modifier = Modifier.weight(1f),
                    )
                    QuickLinkCard(
                        label = "Budgets",
                        icon = Icons.Filled.Savings,
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.weight(1f),
                    )
                    QuickLinkCard(
                        label = "Stats",
                        icon = Icons.Filled.EditCalendar,
                        onClick = onNavigateToStats,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Recent transactions",
                    action = {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { onNavigateToTransactions() },
                        )
                    },
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No transactions yet",
                        subtitle = "Tap Expense or Income above to add your first entry.",
                    )
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { t ->
                    TransactionRow(
                        title = t.note.ifBlank { if (t.isExpense) "Expense" else "Income" },
                        subtitle = DateUtils.shortDate(t.date),
                        icon = null,
                        iconColor = if (t.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        amount = t.amount,
                        symbol = symbol,
                        isExpense = t.isExpense,
                        onClick = { onEditTransaction(t.id) },
                    )
                }
            }

            if (state.budgets.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Budgets",
                        action = {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { onNavigateToBudgets() },
                            )
                        },
                    )
                }
                items(state.budgets.take(3), key = { it.budget.id }) { b ->
                    BudgetProgressRow(
                        label = b.category?.name ?: "Budget",
                        spent = b.spent,
                        limit = b.budget.amount,
                        symbol = symbol,
                        color = Color(b.category?.color ?: 0xFF4CAF50),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinkCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BudgetProgressRow(
    label: String,
    spent: Long,
    limit: Long,
    symbol: String,
    color: Color,
) {
    val progress = if (limit > 0) (spent.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val overspent = spent > limit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(icon = null, color = color, size = 36)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${MoneyFormatter.format(spent)} / ${MoneyFormatter.format(limit)} $symbol",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (overspent) MaterialTheme.colorScheme.error else color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
