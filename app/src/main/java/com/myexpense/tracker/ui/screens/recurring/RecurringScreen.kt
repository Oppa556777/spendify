package com.myexpense.tracker.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.myexpense.tracker.data.model.RecurringFrequency
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.RecurringUiState
import com.myexpense.tracker.ui.viewmodel.RecurringViewModel
import com.myexpense.tracker.ui.viewmodel.RecurringRuleView
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringRuleView?>(null) }
    var pendingDelete by remember { mutableStateOf<RecurringRuleView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showSheet = true
                },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add rule", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.rules.isEmpty()) {
            EmptyState(
                title = "No recurring transactions",
                subtitle = "Set up rules for rent, salary, subscriptions and more.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(state.rules, key = { it.rule.id }) { ruleView ->
                RecurringRow(
                    ruleView = ruleView,
                    symbol = state.currencySymbol,
                    onToggle = { active -> viewModel.toggleActive(ruleView.rule.id, active) },
                    onClick = {
                        editing = ruleView
                        showSheet = true
                    },
                    onDelete = { pendingDelete = ruleView },
                )
            }
        }
    }

    pendingDelete?.let { ruleView ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete rule?") },
            text = { Text("Delete \"${ruleView.rule.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(ruleView.rule.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showSheet) {
        AddEditRecurringSheet(
            existing = editing,
            state = state,
            onDismiss = { showSheet = false },
            onSave = { id, title, amountMinor, type, categoryId, accountId, frequency, interval ->
                viewModel.save(id, title, amountMinor, type, categoryId, accountId, frequency, interval)
                showSheet = false
            },
        )
    }
}

@Composable
private fun RecurringRow(
    ruleView: RecurringRuleView,
    symbol: String,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val rule = ruleView.rule
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        ruleView.categoryName,
                        "${rule.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} · every ${rule.interval}",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Next due: ${DateUtils.mediumDate(Instant.ofEpochMilli(ruleView.nextDueDate).atZone(ZoneOffset.UTC).toLocalDate())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$symbol${MoneyFormatter.format(ruleView.amountMinor)}",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
            Switch(checked = rule.isActive, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditRecurringSheet(
    existing: RecurringRuleView?,
    state: RecurringUiState,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, amountMinor: Long, type: TransactionType, categoryId: Long, accountId: Long, frequency: RecurringFrequency, interval: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var title by remember { mutableStateOf(existing?.rule?.title ?: "") }
    var amount by remember { mutableStateOf(existing?.let { MoneyFormatter.format(it.amountMinor) } ?: "") }
    var type by remember { mutableStateOf(existing?.rule?.type ?: TransactionType.EXPENSE) }
    var categoryId by remember {
        mutableStateOf(
            existing?.rule?.categoryId
                ?: state.expenseCategories.firstOrNull()?.id ?: 0L
        )
    }
    var accountId by remember {
        mutableStateOf(existing?.rule?.accountId ?: state.accounts.firstOrNull()?.id ?: 0L)
    }
    var frequency by remember { mutableStateOf(existing?.rule?.frequency ?: RecurringFrequency.MONTHLY) }
    var interval by remember { mutableStateOf(existing?.rule?.interval ?: 1) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (existing == null) "New recurring rule" else "Edit recurring rule",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text(state.currencySymbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Transfer",
                ).forEach { (t, label) ->
                    Button(
                        onClick = { type = t },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (type == t) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (type == t) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (type != TransactionType.TRANSFER) {
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.expenseCategories.forEach { cat ->
                        val selected = categoryId == cat.id
                        Surface(
                            modifier = Modifier.clickable { categoryId = cat.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
            Text("Account", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.accounts.forEach { account ->
                    val selected = accountId == account.id
                    Surface(
                        modifier = Modifier.clickable { accountId = account.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Text("Frequency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecurringFrequency.entries.forEach { freq ->
                    val selected = frequency == freq
                    Surface(
                        modifier = Modifier.clickable { frequency = freq },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            freq.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Every", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = interval.toString(),
                    onValueChange = { interval = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1 },
                    label = { Text("Interval") },
                    singleLine = true,
                    modifier = Modifier.padding(horizontal = 8.dp).size(width = 90.dp, height = 56.dp),
                )
                Text(frequency.name.lowercase() + "s", style = MaterialTheme.typography.bodyMedium)
            }
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    if (title.isBlank()) { error = "Enter a title"; return@Button }
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value <= 0) { error = "Enter an amount"; return@Button }
                    if (accountId == 0L) { error = "Choose an account"; return@Button }
                    if (type != TransactionType.TRANSFER && categoryId == 0L) { error = "Choose a category"; return@Button }
                    onSave(
                        existing?.rule?.id ?: 0L,
                        title.trim(),
                        (value * 100).toLong().coerceAtLeast(1),
                        type,
                        if (type == TransactionType.TRANSFER) categoryId else categoryId,
                        accountId,
                        frequency,
                        interval.coerceAtLeast(1),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Rule", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
