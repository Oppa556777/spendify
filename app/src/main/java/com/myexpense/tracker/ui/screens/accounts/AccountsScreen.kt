package com.myexpense.tracker.ui.screens.accounts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.components.HeroBalanceCard
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.viewmodel.AccountsViewModel
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    var editing by remember { mutableStateOf<Account?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add account")
            }
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
            state.error?.let { error ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            item {
                HeroBalanceCard(
                    title = "Total balance",
                    balanceText = MoneyFormatter.formatWithSymbol(state.totalBalance, state.currencySymbol),
                    subtitle = "${state.accounts.size} account(s)",
                    modifier = Modifier.fillMaxWidth(),
                    gradient = listOf(Color(0xFF37474F), Color(0xFF546E7A)),
                )
            }

            if (state.accounts.isEmpty()) {
                item {
                    EmptyState(
                        title = "No accounts yet",
                        subtitle = "Add your cash, bank, card or wallet accounts.",
                    )
                }
            } else {
                items(state.accounts, key = { it.account.id }) { accountWithBalance ->
                    val account = accountWithBalance.account
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editing = account
                                showDialog = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryIcon(icon = account.icon, color = Color(account.color), size = 44)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = MoneyFormatter.formatWithSymbol(accountWithBalance.balance, state.currencySymbol),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = AmountFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                ),
                            )
                            IconButton(onClick = { viewModel.delete(account) }) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AccountDialog(
            account = editing,
            onDismiss = { showDialog = false },
            onSave = { account ->
                viewModel.save(account)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    var balance by remember { mutableStateOf(if (account != null) MoneyFormatter.format(account.balance) else "") }
    var color by remember { mutableStateOf(account?.color ?: Palette.colors.first()) }
    var icon by remember { mutableStateOf(account?.icon ?: "account_balance_wallet") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "New Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Balance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.colors.take(8).forEach { c ->
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { color = c },
                            shape = RoundedCornerShape(50),
                            color = Color(c),
                        ) {
                            if (color == c) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val amount = balance.toDoubleOrNull() ?: 0.0
                    onSave(
                        Account(
                            id = account?.id ?: 0,
                            name = name.trim(),
                            type = type,
                            balance = (amount * 100).toLong(),
                            currency = account?.currency ?: "INR",
                            color = color,
                            icon = icon,
                            isDefault = account?.isDefault ?: false,
                            createdAt = account?.createdAt ?: System.currentTimeMillis(),
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

/** Small color palette used across the app. */
object Palette {
    val colors = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5,
        0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300,
        0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF757575, 0xFF546E7A,
    )
}
