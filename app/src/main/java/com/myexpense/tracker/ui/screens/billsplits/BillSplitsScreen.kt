package com.myexpense.tracker.ui.screens.billsplits

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
import com.myexpense.tracker.data.repository.BillSplitView
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.BillSplitsViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitsScreen(
    onBack: () -> Unit,
    viewModel: BillSplitsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BillSplitView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Bill", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Color(0xFF6C63FF),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Split a bill", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.splits.isEmpty()) {
            EmptyState(
                title = "No split bills yet",
                subtitle = "Split dinner, rent, trips — whoever owes what, tracked here.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.splits, key = { it.id }) { split ->
                SplitBillCard(
                    split = split,
                    symbol = symbol,
                    onTogglePaid = { memberId, paid -> viewModel.togglePaid(memberId, paid) },
                    onDelete = { pendingDelete = split },
                )
            }
        }
    }

    pendingDelete?.let { split ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete split?") },
            text = { Text("Delete \"${split.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSplit(split.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showSheet) {
        CreateSplitSheet(
            people = state.people,
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { title, total, date, payerId, members ->
                viewModel.saveSplit(title, total, date, null, payerId, members)
                showSheet = false
            },
        )
    }
}

@Composable
private fun SplitBillCard(
    split: BillSplitView,
    symbol: String,
    onTogglePaid: (Long, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = split.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = DateUtils.fullDate(Instant.ofEpochMilli(split.date).atZone(ZoneOffset.UTC).toLocalDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$symbol${MoneyFormatter.format(split.total)}",
                    style = MaterialTheme.typography.titleMedium,
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
            }

            split.payerName?.let {
                Text(
                    text = "Paid by $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6C63FF),
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))
            split.members.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePaid(member.id, !member.isPaid) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(member.avatarColor)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = member.personName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "  ${member.personName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$symbol${MoneyFormatter.format(member.share)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (member.isPaid) Color(0xFF43A047) else MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (member.isPaid) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Paid",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            if (split.allSettled) {
                Text(
                    text = "✓ Fully settled",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Text(
                    text = "${MoneyFormatter.format(split.paidTotal)} of ${MoneyFormatter.format(split.total)} settled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CREATE SPLIT SHEET
// ─────────────────────────────────────────────────────────────────────────────

private enum class SplitMethod { EQUAL, CUSTOM, PERCENT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateSplitSheet(
    people: List<com.myexpense.tracker.data.model.Person>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (title: String, total: Long, date: Long, payerId: Long?, members: List<Triple<Long?, String, Long>>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var title by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(SplitMethod.EQUAL) }
    var participants by remember { mutableStateOf(listOf<Long?>()) }
    var customShares by remember { mutableStateOf(mapOf<Long?, String>()) }
    var percents by remember { mutableStateOf(mapOf<Long?, String>()) }
    var payerId by remember { mutableStateOf<Long?>(null) }
    var newPersonName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun addParticipant() {
        val name = newPersonName.trim()
        if (name.isBlank()) {
            val next = people.firstOrNull { it.id !in participants }
            if (next != null) participants = participants + next.id
        } else {
            participants = participants + null  // placeholder resolved on save
            newPersonName = ""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Split a bill", style = MaterialTheme.typography.headlineSmall, fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Dinner at Olive Garden") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = total,
                onValueChange = { total = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Total bill amount") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Participants
            Text("Participants", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPersonName,
                    onValueChange = { newPersonName = it },
                    placeholder = { Text("New person name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::addParticipant) { Text("Add") }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                people.forEach { person ->
                    val selected = person.id in participants
                    Surface(
                        modifier = Modifier.clickable {
                            participants = if (selected) participants - person.id else participants + person.id
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            person.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            if (participants.isEmpty()) {
                Text(
                    text = "Add at least one participant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Split method
            Text("Split method", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    SplitMethod.EQUAL to "Equal",
                    SplitMethod.CUSTOM to "Custom",
                    SplitMethod.PERCENT to "Percentage",
                ).forEach { (m, label) ->
                    Button(
                        onClick = { method = m },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (method == m) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (method == m) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Who paid
            Text("Who paid?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.clickable { payerId = null },
                    shape = RoundedCornerShape(50),
                    color = if (payerId == null) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text("Nobody", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                people.filter { it.id in participants }.forEach { person ->
                    val selected = payerId == person.id
                    Surface(
                        modifier = Modifier.clickable { payerId = person.id },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color(0xFF6C63FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(person.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val totalValue = total.toDoubleOrNull() ?: 0.0
                    if (totalValue <= 0) { error = "Enter a total amount"; return@Button }
                    if (participants.isEmpty()) { error = "Add participants"; return@Button }
                    val totalMinor = (totalValue * 100).toLong()
                    val count = participants.size
                    val memberShares = participants.mapIndexed { index, id ->
                        val share = when (method) {
                            SplitMethod.EQUAL -> totalMinor / count
                            SplitMethod.CUSTOM -> {
                                val s = customShares[id]?.toDoubleOrNull() ?: 0.0
                                (s * 100).toLong()
                            }
                            SplitMethod.PERCENT -> {
                                val p = percents[id]?.toDoubleOrNull() ?: 0.0
                                (totalMinor * p / 100).toLong()
                            }
                        }
                        val name = people.firstOrNull { it.id == id }?.name ?: "Person ${index + 1}"
                        Triple(id, name, share)
                    }
                    if (method != SplitMethod.EQUAL && memberShares.sumOf { it.third } != totalMinor) {
                        error = "Shares don't add up to the total"
                        return@Button
                    }
                    onSave(title.trim().ifBlank { "Split bill" }, totalMinor, System.currentTimeMillis(), payerId, memberShares)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Split", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
