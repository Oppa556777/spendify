package com.myexpense.tracker.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.CategoryChip
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.MonthSelector
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.viewmodel.TransactionsViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "In: " + MoneyFormatter.formatWithSymbol(state.monthIncome, symbol),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Out: " + MoneyFormatter.formatWithSymbol(state.monthExpense, symbol),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Type filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                item { FilterPill("All", state.typeFilter == null) { viewModel.setTypeFilter(null) } }
                item { FilterPill("Expense", state.typeFilter == TransactionType.EXPENSE) { viewModel.setTypeFilter(TransactionType.EXPENSE) } }
                item { FilterPill("Income", state.typeFilter == TransactionType.INCOME) { viewModel.setTypeFilter(TransactionType.INCOME) } }
            }

            // Category filter
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    item { FilterPill("All categories", state.categoryFilter == null) { viewModel.setCategoryFilter(null) } }
                    items(state.categories, key = { it.id }) { category ->
                        CategoryChip(
                            category = category,
                            selected = state.categoryFilter == category.id,
                            onClick = {
                                viewModel.setCategoryFilter(if (state.categoryFilter == category.id) null else category.id)
                            },
                        )
                    }
                }
            }

            if (state.transactions.isEmpty()) {
                EmptyState(
                    title = "No transactions",
                    subtitle = "Nothing matches these filters for ${DateUtils.monthYear(state.month)}.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val grouped = state.transactions.groupBy { it.date }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    grouped.forEach { (date, list) ->
                        item(key = "date-$date") {
                            Text(
                                text = DateUtils.fullDate(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(list, key = { it.id }) { t ->
                            val category = state.categories.firstOrNull { it.id == t.categoryId }
                            TransactionRow(
                                title = t.title.ifBlank { category?.name ?: if (t.isExpense) "Expense" else "Income" },
                                subtitle = category?.name
                                    ?: (state.accounts.firstOrNull { it.id == t.accountId }?.name ?: "Uncategorized"),
                                icon = category?.icon,
                                iconColor = Color(category?.color ?: if (t.isExpense) 0xFFC62828 else 0xFF2E7D32),
                                amount = t.amount,
                                symbol = symbol,
                                isExpense = t.isExpense,
                                onClick = { onEditTransaction(t.id) },
                                trailing = {
                                    IconButton(onClick = { viewModel.deleteTransaction(t.id) }) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = background,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
