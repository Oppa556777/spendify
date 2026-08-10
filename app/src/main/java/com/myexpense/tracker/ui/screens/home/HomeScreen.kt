package com.myexpense.tracker.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.HomePeriod
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.viewmodel.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeAllTransactions: () -> Unit,
    onSeeAllBudgets: () -> Unit,
    onSeeAllStats: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onTransfer: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol

    var detailTransactionId by remember { mutableStateOf<Long?>(null) }
    var dismissedReminders by remember { mutableStateOf(setOf<Long>()) }
    var customStage by remember { mutableStateOf<CustomStage?>(null) }
    var customFrom by remember { mutableStateOf<LocalDate?>(null) }

    val detailTransaction = state.recentTransactions.firstOrNull { it.id == detailTransactionId }

    val categoryMap = state.categories.associateBy { it.id }
    val accountMap = state.accounts.associate { it.account.id to it.account.name }

    val accounts = state.accounts
    val accountPages = buildList {
        add(null to "All accounts")
        accounts.forEach { add(it.account.id to it.account.name) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item {
                HeroBalanceCard(
                    greeting = state.greeting,
                    dateText = state.dateText,
                    balance = state.totalBalance,
                    symbol = symbol,
                    hidden = state.balanceHidden,
                    income = state.income,
                    expense = state.expense,
                    accountPages = accountPages,
                    notificationCount = state.notificationCount,
                    onPageChange = { page ->
                        val id = accountPages.getOrNull(page)?.first
                        viewModel.selectAccount(id)
                    },
                    onToggleVisibility = viewModel::toggleBalanceVisibility,
                    onBell = { /* notifications sheet could go here */ },
                    onSearch = onOpenSearch,
                    onAvatar = onOpenSettings,
                )
            }

            item {
                PeriodFilterChips(
                    selected = state.period,
                    onSelect = { period ->
                        if (period == HomePeriod.CUSTOM) {
                            customStage = CustomStage.FROM
                        } else {
                            viewModel.setPeriod(period)
                        }
                    },
                )
            }

            // ── Spending overview ───────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 12.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Spending Overview",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "See All →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable(onClick = onSeeAllStats)
                                    .padding(6.dp),
                            )
                        }
                        MiniBarChart(
                            stats = state.dailyStats,
                            symbol = symbol,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ── Quick actions ───────────────────────────────────────────────
            item {
                QuickActionsRow(
                    onExpense = onAddExpense,
                    onIncome = onAddIncome,
                    onTransfer = onTransfer,
                    onReports = onSeeAllStats,
                )
            }

            // ── Budgets ─────────────────────────────────────────────────────
            item {
                HomeSectionHeader(
                    title = "Budgets",
                    actionText = "View All →",
                    onAction = onSeeAllBudgets,
                )
            }
            if (state.budgets.isEmpty()) {
                item {
                    Text(
                        text = "No budgets yet. Tap View All to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                items(state.budgets.take(3), key = { it.budget.id }) { budget ->
                    BudgetProgressCardView(
                        budget = budget,
                        symbol = symbol,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Recent transactions ─────────────────────────────────────────
            item {
                HomeSectionHeader(
                    title = "Recent Transactions",
                    actionText = "See All →",
                    onAction = onSeeAllTransactions,
                )
            }
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No transactions yet",
                        subtitle = "Tap + Add Expense or Income to get started.",
                    )
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { t ->
                    val category = t.categoryId?.let { categoryMap[it] }
                    SwipeTransactionRow(
                        transaction = t,
                        symbol = symbol,
                        categoryName = category?.name ?: if (t.type.name == "TRANSFER") "Transfer" else "Uncategorized",
                        categoryIcon = category?.icon,
                        categoryColor = Color(category?.color ?: if (t.isExpense) 0xFFE53935 else 0xFF43A047),
                        onClick = { detailTransactionId = t.id },
                        onEdit = { onEditTransaction(t.id) },
                        onDelete = { viewModel.deleteTransaction(t.id) },
                    )
                }
            }

            // ── Savings goals ───────────────────────────────────────────────
            item {
                HomeSectionHeader(title = "My Goals")
            }
            if (state.goals.isEmpty()) {
                item {
                    Text(
                        text = "Create savings goals to watch your money grow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.goals, key = { it.goal.id }) { goal ->
                            GoalCardView(
                                goal = goal,
                                symbol = symbol,
                                modifier = Modifier.width(190.dp),
                            )
                        }
                    }
                }
            }

            // ── Subscription reminders ──────────────────────────────────────
            items(state.subscriptionsDue, key = { it.id }) { reminder ->
                if (reminder.id !in dismissedReminders) {
                    SubscriptionReminderCard(
                        reminder = reminder,
                        symbol = symbol,
                        onDismiss = { dismissedReminders = dismissedReminders + reminder.id },
                        onPay = { viewModel.markSubscriptionNoted(reminder) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    // ── Transaction detail sheet ─────────────────────────────────────────────
    detailTransaction?.let { t ->
        val category = t.categoryId?.let { categoryMap[it] }
        TransactionDetailSheet(
            transaction = t,
            symbol = symbol,
            categoryName = category?.name ?: if (t.type.name == "TRANSFER") "Transfer" else "Uncategorized",
            categoryIcon = category?.icon,
            categoryColor = Color(category?.color ?: if (t.isExpense) 0xFFE53935 else 0xFF43A047),
            accountName = accountMap[t.accountId] ?: "—",
            onDismiss = { detailTransactionId = null },
            onEdit = {
                detailTransactionId = null
                onEditTransaction(t.id)
            },
            onDelete = {
                detailTransactionId = null
                viewModel.deleteTransaction(t.id)
            },
        )
    }

    // ── Custom period date pickers ───────────────────────────────────────────
    when (customStage) {
        CustomStage.FROM -> DateRangePickerDialog(
            title = "Start date",
            initial = customFrom ?: LocalDate.now().minusDays(30),
            onConfirm = { date ->
                customFrom = date
                customStage = CustomStage.TO
            },
            onDismiss = { customStage = null },
        )
        CustomStage.TO -> DateRangePickerDialog(
            title = "End date",
            initial = LocalDate.now(),
            onConfirm = { date ->
                val from = customFrom ?: date
                viewModel.setCustomRange(minOf(from, date), maxOf(from, date))
                customStage = null
            },
            onDismiss = { customStage = null },
        )
        null -> Unit
    }
}

private enum class CustomStage { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    title: String,
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = datePickerState, title = { Text(title) })
    }
}
