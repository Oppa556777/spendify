package com.myexpense.tracker.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.data.model.AccountType.emoji
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.AccountsViewModel
import com.myexpense.tracker.utils.AllCurrencies
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.IconMap
import com.myexpense.tracker.utils.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onAccountClick: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var editing by remember { mutableStateOf<Account?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Accounts", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                },
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
                Icon(Icons.Filled.Add, contentDescription = "Add account", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Summary header ──────────────────────────────────────────────
            item {
                SummaryHeader(
                    assets = state.summary.assets,
                    liabilities = state.summary.liabilities,
                    netWorth = state.summary.netWorth,
                    symbol = symbol,
                )
            }

            // ── Carousel ────────────────────────────────────────────────────
            if (state.accounts.isNotEmpty()) {
                item {
                    Text(
                        text = "Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                item {
                    AccountCarousel(
                        accounts = state.accounts,
                        symbol = symbol,
                        latest = state.latestTransactions,
                        onAccountClick = onAccountClick,
                    )
                }
            }

            // ── Full list ───────────────────────────────────────────────────
            if (state.accounts.isEmpty()) {
                item {
                    EmptyState(
                        title = "No accounts yet",
                        subtitle = "Tap + to add your first account.",
                    )
                }
            } else {
                item {
                    Text(
                        text = "All accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(state.accounts, key = { it.account.id }) { accountWithBalance ->
                    AccountListRow(
                        accountWithBalance = accountWithBalance,
                        symbol = symbol,
                        onClick = { onAccountClick(accountWithBalance.account.id) },
                        onDelete = { pendingDelete = accountWithBalance.account },
                    )
                }
            }
        }
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete account?") },
            text = { Text("Delete \"${account.name}\"? Its transactions will be moved to another account.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(account)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // ── Add / Edit sheet ───────────────────────────────────────────────────
    if (showSheet) {
        AddEditAccountSheet(
            account = editing,
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { account ->
                viewModel.save(account)
                showSheet = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryHeader(assets: Long, liabilities: Long, netWorth: Long, symbol: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryItem("Total Assets", assets, symbol, Color(0xFF43A047))
            SummaryItem("Total Liabilities", liabilities, symbol, Color(0xFFE53935))
            SummaryItem("Net Worth", netWorth, symbol, Color(0xFF6C63FF))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: Long, symbol: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$symbol${MoneyFormatter.format(value, 0)}",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountCarousel(
    accounts: List<AccountWithBalance>,
    symbol: String,
    latest: Map<Long, com.myexpense.tracker.data.model.Transaction>,
    onAccountClick: (Long) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { accounts.size })
    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 10.dp,
        ) { page ->
            val awb = accounts[page]
            AccountCarouselCard(
                accountWithBalance = awb,
                symbol = symbol,
                lastTransaction = latest[awb.account.id],
                onClick = { onAccountClick(awb.account.id) },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            accounts.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) {
                                Color(0xFF6C63FF)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun AccountCarouselCard(
    accountWithBalance: AccountWithBalance,
    symbol: String,
    lastTransaction: com.myexpense.tracker.data.model.Transaction?,
    onClick: () -> Unit,
) {
    val account = accountWithBalance.account
    val gradient = remember(account.id) {
        listOf(
            Color(account.color),
            blendWithBlack(account.color, 0.35f),
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = account.name,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (account.isDefault) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Default",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.25f),
                ) {
                    Text(
                        text = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            CategoryIcon(
                icon = account.icon,
                color = Color.White,
                size = 48,
                modifier = Modifier.align(Alignment.CenterStart),
            )

            Text(
                text = "$symbol${MoneyFormatter.format(accountWithBalance.balance)}",
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )

            Text(
                text = lastTransaction?.let {
                    "Last: ${it.title.ifBlank { if (it.isExpense) "Expense" else "Income" }} • ${DateUtils.shortDate(it.date)}"
                } ?: "No transactions yet",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

private fun blendWithBlack(color: Long, factor: Float): Color {
    val c = Color(color)
    return Color(
        red = c.red * (1f - factor),
        green = c.green * (1f - factor),
        blue = c.blue * (1f - factor),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST ROW (swipe to delete)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountListRow(
    accountWithBalance: AccountWithBalance,
    symbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) {
        val account = accountWithBalance.account
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIcon(icon = account.icon, color = Color(account.color), size = 42)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${account.type.emoji} ${account.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$symbol${MoneyFormatter.format(accountWithBalance.balance)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (accountWithBalance.balance >= 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD / EDIT ACCOUNT SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditAccountSheet(
    account: Account?,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    var balance by remember { mutableStateOf(if (account != null) MoneyFormatter.format(account.balance) else "") }
    var currency by remember { mutableStateOf(account?.currency ?: "INR") }
    var color by remember { mutableStateOf(account?.color ?: Palette.colors.first()) }
    var icon by remember { mutableStateOf(account?.icon ?: "account_balance_wallet") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    val currencyInfo = AllCurrencies.firstOrNull { it.code == currency } ?: AllCurrencies.first()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (account == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )

            // 1. Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                placeholder = { Text("e.g. SBI Bank, Cash, Wallet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Type grid
            Text("Account type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountType.entries.forEach { t ->
                    val selected = type == t
                    Surface(
                        modifier = Modifier.clickable { type = t },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            Color(0xFF6C63FF).copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(t.emoji, fontSize = 16.sp)
                            Text(
                                t.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            // 3. Starting balance
            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                label = { Text("Starting balance") },
                prefix = { Text(currencyInfo.symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 4. Currency
            Text("Currency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            var currencyOpen by remember { mutableStateOf(false) }
            androidx.compose.material3.ExposedDropdownMenuBox(
                expanded = currencyOpen,
                onExpandedChange = { currencyOpen = it },
            ) {
                OutlinedTextField(
                    value = "${currencyInfo.symbol}  ${currencyInfo.code} — ${currencyInfo.name}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = {
                        androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyOpen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, enabled = true),
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = currencyOpen,
                    onDismissRequest = { currencyOpen = false },
                ) {
                    AllCurrencies.forEach { info ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("${info.symbol}  ${info.code} — ${info.name}") },
                            onClick = {
                                currency = info.code
                                currencyOpen = false
                            },
                        )
                    }
                }
            }

            // 5. Color picker (20 presets + random custom)
            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Palette.colors.forEach { c ->
                    Surface(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { color = c },
                        shape = CircleShape,
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
                // custom random
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            color = (0xFF000000 or (0..0xFFFFFF).random().toLong())
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎲", fontSize = 14.sp)
                    }
                }
            }

            // 6. Icon picker
            Text("Icon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(IconMap.all.entries.toList()) { (key, vector) ->
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { icon = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (icon == key) {
                            Color(color).copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = vector,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (icon == key) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 7. Default toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Set as default account",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Enter an account name"
                        return@Button
                    }
                    val amount = balance.toDoubleOrNull() ?: 0.0
                    onSave(
                        Account(
                            id = account?.id ?: 0,
                            name = name.trim(),
                            type = type,
                            balance = (amount * 100).toLong(),
                            currency = currency,
                            color = color,
                            icon = icon,
                            isDefault = isDefault,
                            createdAt = account?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text("Save Account", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Small color palette used across the app. */
object Palette {
    val colors = listOf(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5,
        0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300,
        0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF757575, 0xFF546E7A,
    )
}
