package com.myexpense.tracker.ui.screens.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.myexpense.tracker.data.model.Loan
import com.myexpense.tracker.data.model.LoanStatus
import com.myexpense.tracker.data.model.LoanType
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.LoansViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    onBack: () -> Unit,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Loan?>(null) }
    var paymentLoan by remember { mutableStateOf<Loan?>(null) }

    val lent = state.loans.filter { it.type == LoanType.LENT }
    val borrowed = state.loans.filter { it.type == LoanType.BORROWED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add loan", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.loans.isEmpty()) {
            EmptyState(
                title = "No loans yet",
                subtitle = "Track money you lent or borrowed.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Summary header ──────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(
                        label = "Total Lent",
                        value = state.totalLent,
                        symbol = symbol,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryTile(
                        label = "Total Borrowed",
                        value = state.totalBorrowed,
                        symbol = symbol,
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── I Lent ──────────────────────────────────────────────────────
            item {
                SectionTitle("I Lent", Color(0xFF3B82F6))
            }
            if (lent.isEmpty()) {
                item {
                    Text(
                        text = "Nothing lent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(lent, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        symbol = symbol,
                        onSettle = { viewModel.markSettled(loan.id) },
                        onPayment = { paymentLoan = loan },
                        onDelete = { pendingDelete = loan },
                    )
                }
            }

            // ── I Borrowed ──────────────────────────────────────────────────
            item {
                SectionTitle("I Borrowed", Color(0xFFF57C00))
            }
            if (borrowed.isEmpty()) {
                item {
                    Text(
                        text = "Nothing borrowed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(borrowed, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        symbol = symbol,
                        onSettle = { viewModel.markSettled(loan.id) },
                        onPayment = { paymentLoan = loan },
                        onDelete = { pendingDelete = loan },
                    )
                }
            }
        }
    }

    // ── Payment dialog ─────────────────────────────────────────────────────
    paymentLoan?.let { loan ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { paymentLoan = null },
            title = { Text(if (loan.type == LoanType.LENT) "Record repayment" else "Record payment") },
            text = {
                Column {
                    Text(
                        text = "Remaining: $symbol${MoneyFormatter.format(loan.remaining)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount") },
                        prefix = { Text(symbol) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value > 0) {
                        viewModel.addPayment(loan.id, (value * 100).toLong())
                        paymentLoan = null
                    }
                }) { Text("Record") }
            },
            dismissButton = {
                TextButton(onClick = { paymentLoan = null }) { Text("Cancel") }
            },
        )
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    pendingDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete loan?") },
            text = { Text("Delete the ${loan.type.name.lowercase()} loan with ${loan.personName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(loan.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // ── Add loan sheet ─────────────────────────────────────────────────────
    if (showSheet) {
        AddLoanSheet(
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { loan ->
                viewModel.save(loan)
                showSheet = false
            },
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: Long, symbol: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            Text(
                text = "$symbol${MoneyFormatter.format(value)}",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = AmountFontFamily,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "  $text",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun LoanCard(
    loan: Loan,
    symbol: String,
    onSettle: () -> Unit,
    onPayment: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when (loan.status) {
        LoanStatus.ACTIVE -> Color(0xFF43A047)
        LoanStatus.OVERDUE -> Color(0xFFE53935)
        LoanStatus.SETTLED -> Color(0xFF2E7D32)
    }
    val isLent = loan.type == LoanType.LENT
    val accent = if (isLent) Color(0xFF3B82F6) else Color(0xFFF57C00)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = loan.personName.take(1).uppercase(),
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
                        text = loan.personName,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    loan.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Status chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = loan.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Original",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$symbol${MoneyFormatter.format(loan.amount)}",
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$symbol${MoneyFormatter.format(loan.remaining)}",
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.remaining > 0) accent else Color(0xFF2E7D32),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DateUtils.shortDate(Instant.ofEpochMilli(loan.date).atZone(ZoneOffset.UTC).toLocalDate()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = loan.dueDate?.let {
                            DateUtils.shortDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        } ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!loan.isSettled) {
                    OutlinedButton(
                        onClick = onPayment,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Partial Payment")
                    }
                    Button(
                        onClick = onSettle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Mark Settled")
                    }
                } else {
                    Text(
                        text = "✓ Settled",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2E7D32),
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD LOAN SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Loan) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var type by remember { mutableStateOf(LoanType.LENT) }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
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
                text = "New Loan",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    LoanType.LENT to "I Lent",
                    LoanType.BORROWED to "I Borrowed",
                ).forEach { (t, label) ->
                    Button(
                        onClick = { type = t },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == t) {
                                if (t == LoanType.LENT) Color(0xFF3B82F6) else Color(0xFFF57C00)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (type == t) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedTextField(
                value = person,
                onValueChange = { person = it },
                label = { Text("Person name") },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dueDate?.let {
                        "Due: ${DateUtils.mediumDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}"
                    } ?: "No due date",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    dueDate = if (dueDate == null) System.currentTimeMillis() + 30L * 86_400_000L else null
                }) {
                    Text(if (dueDate == null) "Set due date" else "Clear")
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
                    if (person.isBlank()) { error = "Enter a person name"; return@Button }
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value <= 0) { error = "Enter an amount"; return@Button }
                    onSave(
                        Loan(
                            type = type,
                            personName = person.trim(),
                            amount = (value * 100).toLong().coerceAtLeast(1),
                            date = System.currentTimeMillis(),
                            dueDate = dueDate,
                            note = note.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Loan", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
