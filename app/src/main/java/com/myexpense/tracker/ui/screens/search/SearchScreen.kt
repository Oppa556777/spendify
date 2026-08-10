package com.myexpense.tracker.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.viewmodel.SearchSort
import com.myexpense.tracker.ui.viewmodel.SearchViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showFilters by remember { mutableStateOf(false) }
    val categoryMap = state.categories.associateBy { it.id }
    val accountMap = state.accounts.associateBy { it.id }
    val personMap = state.people.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Text("Filters", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                },
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
            // ── Search bar ──────────────────────────────────────────────────
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search title, notes, categories…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            // ── Sort row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sort:", style = MaterialTheme.typography.labelMedium)
                SearchSort.entries.forEach { s ->
                    FilterChip(
                        selected = state.sort == s,
                        onClick = { viewModel.setSort(s) },
                        label = { Text(s.label) },
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            // ── Recent searches ─────────────────────────────────────────────
            if (state.history.isNotEmpty() && state.query.isBlank()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.history.forEach { h ->
                        Surface(
                            modifier = Modifier.clickable { viewModel.useHistory(h) },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                "🕐 $h",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                TextButton(
                    onClick = viewModel::clearHistory,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Clear history")
                }
            }

            // ── Filter panel ────────────────────────────────────────────────
            if (showFilters) {
                FilterPanel(
                    state = state,
                    onCategory = viewModel::setCategory,
                    onAccount = viewModel::setAccount,
                    onPerson = viewModel::setPerson,
                    onTag = viewModel::setTag,
                    onMin = viewModel::setMin,
                    onMax = viewModel::setMax,
                    onClear = viewModel::clearFilters,
                )
            }

            // ── Results ─────────────────────────────────────────────────────
            when {
                state.query.isBlank() && state.filters == com.myexpense.tracker.ui.viewmodel.SearchFilters() ->
                    EmptyState(
                        title = "Search your transactions",
                        subtitle = "Type a note, title or category to find entries. Tap Filters for advanced search.",
                    )
                state.results.isEmpty() ->
                    EmptyState(
                        title = "No results",
                        subtitle = "Nothing matched your search.",
                    )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.results, key = { it.id }) { t ->
                        val category = t.categoryId?.let { categoryMap[it] }
                        SearchResultRow(
                            transaction = t,
                            symbol = symbol,
                            query = state.query,
                            categoryName = category?.name ?: if (t.type.name == "TRANSFER") "Transfer" else "Uncategorized",
                            categoryIcon = category?.icon,
                            categoryColor = Color(category?.color ?: if (t.isExpense) 0xFFE53935 else 0xFF43A047),
                            personName = t.personId?.let { personMap[it]?.name },
                            onClick = { onEditTransaction(t.id) },
                            onDelete = { viewModel.delete(t.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: com.myexpense.tracker.ui.viewmodel.SearchUiState,
    onCategory: (Long?) -> Unit,
    onAccount: (Long?) -> Unit,
    onPerson: (Long?) -> Unit,
    onTag: (Long?) -> Unit,
    onMin: (Long?) -> Unit,
    onMax: (Long?) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Advanced filters", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        Text("Category", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = state.filters.categoryId == null, onClick = { onCategory(null) }, label = { Text("Any") })
            state.categories.forEach { c ->
                FilterChip(
                    selected = state.filters.categoryId == c.id,
                    onClick = { onCategory(c.id) },
                    label = { Text(c.name) },
                )
            }
        }
        Text("Account", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = state.filters.accountId == null, onClick = { onAccount(null) }, label = { Text("Any") })
            state.accounts.forEach { a ->
                FilterChip(
                    selected = state.filters.accountId == a.id,
                    onClick = { onAccount(a.id) },
                    label = { Text(a.name) },
                )
            }
        }
        Text("Tags", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = state.filters.tagId == null, onClick = { onTag(null) }, label = { Text("Any") })
            state.tags.forEach { tag ->
                FilterChip(
                    selected = state.filters.tagId == tag.id,
                    onClick = { onTag(tag.id) },
                    label = { Text(tag.name) },
                )
            }
        }
        Text("Person", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = state.filters.personId == null, onClick = { onPerson(null) }, label = { Text("Any") })
            state.people.forEach { p ->
                FilterChip(
                    selected = state.filters.personId == p.id,
                    onClick = { onPerson(p.id) },
                    label = { Text(p.name) },
                )
            }
        }
        Text("Amount range", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountFilterField(
                value = state.filters.minAmount,
                label = "Min",
                symbol = state.currencySymbol,
                onChange = onMin,
                modifier = Modifier.weight(1f),
            )
            AmountFilterField(
                value = state.filters.maxAmount,
                label = "Max",
                symbol = state.currencySymbol,
                onChange = onMax,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AmountFilterField(
    value: Long?,
    label: String,
    symbol: String,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value?.let { MoneyFormatter.format(it) } ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { c -> c.isDigit() || c == '.' }
            onChange((text.toDoubleOrNull() ?: 0.0).takeIf { v -> v > 0 }?.let { (it * 100).toLong() })
        },
        label = { Text(label) },
        prefix = { Text(symbol) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchResultRow(
    transaction: Transaction,
    symbol: String,
    query: String,
    categoryName: String,
    categoryIcon: String?,
    categoryColor: Color,
    personName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(icon = categoryIcon, color = categoryColor, size = 40)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight(transaction.title.ifBlank { categoryName }, query),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        categoryName,
                        personName?.let { "👤 $it" },
                        DateUtils.fullDate(transaction.date),
                    ).joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = (if (transaction.isExpense) "-" else "+") + "$symbol" + MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Highlights query matches in [text]. */
private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var index = 0
        val lower = text.lowercase()
        val q = query.lowercase()
        while (index < text.length) {
            val match = lower.indexOf(q, index)
            if (match < 0) {
                append(text.substring(index))
                break
            }
            append(text.substring(index, match))
            withStyle(SpanStyle(background = Color(0xFFFFF176), fontWeight = FontWeight.Bold)) {
                append(text.substring(match, match + q.length))
            }
            index = match + q.length
        }
    }
}
