package com.myexpense.tracker.ui.screens.subscriptions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.BillingCycle
import com.myexpense.tracker.data.model.DueUrgency
import com.myexpense.tracker.data.model.Subscription
import com.myexpense.tracker.data.model.urgency
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.screens.accounts.Palette
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.SubscriptionsViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Subscriptions", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add subscription", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Summary header ──────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SummaryStat("Monthly", state.monthlyTotal, symbol, Color(0xFF6C63FF))
                        SummaryStat("Yearly", state.yearlyTotal, symbol, Color(0xFF3B82F6))
                        SummaryStat(
                            "Active",
                            state.activeCount,
                            symbol,
                            Color(0xFF43A047),
                            isCount = true,
                        )
                    }
                }
            }

            // ── Category filter ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        modifier = Modifier.clickable { viewModel.setFilter(null) },
                        shape = RoundedCornerShape(50),
                        color = if (state.filter == null) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                        border = if (state.filter != null) {
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        } else null,
                    ) {
                        Text(
                            "All",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.filter == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                    state.categoryNames.forEach { category ->
                        val selected = state.filter == category
                        Surface(
                            modifier = Modifier.clickable { viewModel.setFilter(if (selected) null else category) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                            border = if (!selected) {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            } else null,
                        ) {
                            Text(
                                category,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            if (state.subscriptions.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing here yet",
                        subtitle = "Track Netflix, Spotify, gym memberships and more.",
                        illustrationRes = com.myexpense.tracker.R.drawable.ic_empty_calendar,
                        ctaLabel = "Add Subscription",
                        onCta = { showSheet = true },
                    )
                }
            } else {
                items(state.subscriptions, key = { it.id }) { subscription ->
                    SubscriptionCard(
                        subscription = subscription,
                        symbol = symbol,
                        onToggle = { active -> viewModel.toggleActive(subscription.id, active) },
                        onPay = { viewModel.markNoted(subscription.id) },
                        onDelete = { pendingDelete = subscription },
                    )
                }
            }
        }
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    pendingDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete subscription?") },
            text = { Text("Delete \"${sub.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(sub.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // ── Add sheet ──────────────────────────────────────────────────────────
    if (showSheet) {
        AddSubscriptionSheet(
            symbol = symbol,
            expenseCategories = state.expenseCategories,
            onDismiss = { showSheet = false },
            onSave = { subscription ->
                viewModel.save(subscription)
                showSheet = false
            },
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: Long, symbol: String, color: Color, isCount: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (isCount) "$value" else "$symbol${MoneyFormatter.format(value)}",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    symbol: String,
    onToggle: (Boolean) -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = Color(subscription.color)
    val urgencyColor = when (subscription.urgency) {
        DueUrgency.FAR -> Color(0xFF43A047)
        DueUrgency.SOON -> Color(0xFFF57C00)
        DueUrgency.URGENT -> Color(0xFFE53935)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon: colored circle with first letter
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subscription.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subscription.categoryName ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$symbol${MoneyFormatter.format(subscription.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "/${subscription.billingCycle.name.lowercase().take(3)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Next: ${DateUtils.mediumDate(Instant.ofEpochMilli(subscription.nextDueDate).atZone(ZoneOffset.UTC).toLocalDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = urgencyColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "Due in ${subscription.daysLeft}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (subscription.isActive) {
                    TextButton(onClick = onPay) {
                        Text("Pay")
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (subscription.isActive) "Active" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (subscription.isActive) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(checked = subscription.isActive, onCheckedChange = onToggle)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD SUBSCRIPTION SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddSubscriptionSheet(
    symbol: String,
    expenseCategories: List<com.myexpense.tracker.data.model.Category>,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var categoryId by remember {
        mutableStateOf(
            expenseCategories.firstOrNull { it.name == "Subscriptions" }?.id
                ?: expenseCategories.firstOrNull()?.id
        )
    }
    var color by remember { mutableStateOf(0xFF6C63FF) }
    var reminderDays by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val categoryOptions = expenseCategories

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
                text = "New Subscription",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Service name") },
                placeholder = { Text("e.g. Netflix, Spotify, Gym") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Billing cycle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BillingCycle.entries.forEach { c ->
                    Button(
                        onClick = { cycle = c },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cycle == c) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (cycle == c) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(c.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categoryOptions.forEach { option ->
                    val selected = categoryId == option.id
                    Surface(
                        modifier = Modifier.clickable { categoryId = option.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            Color(0xFF6C63FF).copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Text(
                            option.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Remind me $reminderDays day${if (reminderDays == 1) "" else "s"} before",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { reminderDays = (reminderDays - 1).coerceAtLeast(0) }) {
                    Text("−", fontSize = 18.sp)
                }
                Text(
                    "$reminderDays",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { reminderDays = (reminderDays + 1).coerceAtMost(30) }) {
                    Text("+", fontSize = 18.sp)
                }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Palette.colors.take(10).forEach { c ->
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { color = c },
                        shape = CircleShape,
                        color = Color(c),
                    ) {
                        if (color == c) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(5.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    if (name.isBlank()) { error = "Enter a service name"; return@Button }
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value <= 0) { error = "Enter an amount"; return@Button }
                    val catId = categoryId
                    if (catId == null) { error = "Add an expense category first"; return@Button }
                    onSave(
                        Subscription(
                            name = name.trim(),
                            amount = (value * 100).toLong().coerceAtLeast(1),
                            billingCycle = cycle,
                            nextDueDate = System.currentTimeMillis() + 30L * 86_400_000L,
                            categoryId = catId,
                            categoryName = categoryOptions.firstOrNull { it.id == catId }?.name,
                            color = color,
                            icon = "subscriptions",
                            reminderDays = reminderDays,
                            isActive = true,
                            note = note.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Subscription", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}


