package com.myexpense.tracker.ui.screens.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.AmountField
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryChip
import com.myexpense.tracker.ui.components.TypeSelector
import com.myexpense.tracker.ui.viewmodel.AddEditTransactionViewModel
import com.myexpense.tracker.utils.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val filteredCategories = state.categories.filter { it.type == state.type }
    val selectedAccount = state.accounts.firstOrNull { it.id == state.accountId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId == 0L) "Add Transaction" else "Edit Transaction") },
                navigationIcon = { BackButton(onDone) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeSelector(selected = state.type, onSelect = viewModel::setType, includeTransfer = true)

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Title") },
                placeholder = { Text("e.g. Groceries at the market") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )

            AmountField(
                value = state.amountMinor,
                onValueChange = viewModel::setAmount,
                symbol = state.currencySymbol,
            )

            // Category picker (hidden for transfers)
            if (state.type != com.myexpense.tracker.data.model.TransactionType.TRANSFER) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                if (filteredCategories.isEmpty()) {
                    Text(
                        "No categories for this type yet. Add some in Settings → Categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filteredCategories.forEach { category ->
                            CategoryChip(
                                category = category,
                                selected = state.categoryId == category.id,
                                onClick = { viewModel.setCategoryId(category.id) },
                            )
                        }
                    }
                }
            }

            // Account picker (source account; "From account" for transfers)
            if (state.accounts.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select account",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (state.type == com.myexpense.tracker.data.model.TransactionType.TRANSFER) "From account" else "Account")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false },
                    ) {
                        state.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.setAccountId(account.id)
                                    accountMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Destination account (transfers only)
            if (state.type == com.myexpense.tracker.data.model.TransactionType.TRANSFER && state.accounts.isNotEmpty()) {
                var toMenuExpanded by remember { mutableStateOf(false) }
                val selectedToAccount = state.accounts.firstOrNull { it.id == state.toAccountId }
                ExposedDropdownMenuBox(
                    expanded = toMenuExpanded,
                    onExpandedChange = { toMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedToAccount?.name ?: "Select destination",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To account") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = toMenuExpanded,
                        onDismissRequest = { toMenuExpanded = false },
                    ) {
                        state.accounts.filter { it.id != state.accountId }.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.setToAccountId(account.id)
                                    toMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "  " + DateUtils.fullDate(state.date),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  Save Transaction")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.setDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
