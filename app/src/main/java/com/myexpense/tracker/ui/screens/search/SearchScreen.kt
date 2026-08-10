package com.myexpense.tracker.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.TransactionRow
import com.myexpense.tracker.ui.viewmodel.SearchViewModel
import com.myexpense.tracker.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search notes, categories…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            if (state.query.isBlank()) {
                EmptyState(
                    title = "Search your transactions",
                    subtitle = "Type a note or category name to find entries.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (state.results.isEmpty()) {
                EmptyState(
                    title = "No results",
                    subtitle = "Nothing found for \"${state.query}\".",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.results, key = { it.id }) { t ->
                        TransactionRow(
                            title = t.title.ifBlank { t.note.ifBlank { if (t.isExpense) "Expense" else "Income" } },
                            subtitle = DateUtils.fullDate(t.date),
                            icon = null,
                            iconColor = if (t.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            amount = t.amount,
                            symbol = state.currencySymbol,
                            isExpense = t.isExpense,
                            onClick = { onEditTransaction(t.id) },
                            trailing = {
                                IconButton(onClick = { viewModel.delete(t.id) }) {
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
