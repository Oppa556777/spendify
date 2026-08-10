package com.myexpense.tracker.ui.screens.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.BudgetPeriod
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.screens.accounts.Palette
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class BudgetScope { ALL, SPECIFIC, MULTIPLE }

/**
 * Add/Edit budget bottom sheet: name, scope (all/specific/multiple), amount
 * with a compact calculator keypad, period, alert slider, color, custom dates.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditBudgetSheet(
    budget: Budget?,
    categories: List<Category>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(budget?.name ?: "") }
    var scope by remember {
        mutableStateOf(
            when {
                budget?.categoryId == null -> BudgetScope.ALL
                (budget?.categoryIds?.size ?: 1) > 1 -> BudgetScope.MULTIPLE
                else -> BudgetScope.SPECIFIC
            }
        )
    }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: categories.firstOrNull()?.id) }
    var selectedIds by remember {
        mutableStateOf(
            (budget?.categoryIds?.ifEmpty { budget?.categoryId?.let { listOf(it) } } ?: emptyList()).toMutableSet()
        )
    }
    var amountText by remember {
        mutableStateOf(budget?.let { MoneyFormatter.format(it.limitAmount) } ?: "")
    }
    var period by remember { mutableStateOf(budget?.period ?: BudgetPeriod.MONTHLY) }
    var alertAt by remember { mutableStateOf(budget?.alertAt ?: 80) }
    var color by remember { mutableStateOf(budget?.colorHex?.removePrefix("#")?.toLongOrNull(16)?.let { 0xFF000000 or it } ?: 0xFF6C63FF) }
    var startDate by remember {
        mutableStateOf(budget?.startDate?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: LocalDate.now())
    }
    var endDate by remember { mutableStateOf(budget?.endDate?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (budget == null) "New Budget" else "Edit Budget",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )

            // 1. Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Budget name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Scope
            Text("Budget for", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scope == BudgetScope.ALL,
                    onClick = { scope = BudgetScope.ALL },
                    label = { Text("All Expenses") },
                )
                FilterChip(
                    selected = scope == BudgetScope.SPECIFIC,
                    onClick = { scope = BudgetScope.SPECIFIC },
                    label = { Text("Specific Category") },
                )
                FilterChip(
                    selected = scope == BudgetScope.MULTIPLE,
                    onClick = { scope = BudgetScope.MULTIPLE },
                    label = { Text("Multiple") },
                )
            }
            when (scope) {
                BudgetScope.ALL -> Text(
                    "Covers every expense category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BudgetScope.SPECIFIC -> {
                    if (categories.isEmpty()) {
                        Text("No expense categories yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            categories.forEach { category ->
                                val selected = selectedCategoryId == category.id
                                Surface(
                                    modifier = Modifier.clickable { selectedCategoryId = category.id },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        Color(category.color).copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        CategoryIcon(icon = category.icon, color = Color(category.color), size = 22)
                                        Text(
                                            category.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                BudgetScope.MULTIPLE -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { category ->
                            val selected = category.id in selectedIds
                            Surface(
                                modifier = Modifier.clickable {
                                    if (selected) selectedIds.remove(category.id) else selectedIds.add(category.id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    Color(category.color).copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    CategoryIcon(icon = category.icon, color = Color(category.color), size = 22)
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color(category.color),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (selectedIds.isEmpty()) {
                        Text(
                            "Select at least one category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 3. Amount + compact keypad
            Text("Budget amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            BudgetKeypad(
                display = amountText,
                onInput = { amountText = it },
            )

            // 4. Period
            Text("Period", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BudgetPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(p.label) },
                    )
                }
            }

            // 7. Custom dates
            if (period == BudgetPeriod.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateChipButton(
                        text = "Start: ${DateUtils.chipDate(startDate)}",
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    DateChipButton(
                        text = endDate?.let { "End: ${DateUtils.chipDate(it)}" } ?: "End: None",
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 5. Alert slider
            Text("Alert me at", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$alertAt%",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C63FF),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Slider(
                    value = alertAt.toFloat(),
                    onValueChange = { alertAt = it.toInt() },
                    valueRange = 50f..90f,
                    steps = 7,
                    modifier = Modifier.weight(1f),
                )
            }

            // 6. Color
            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Palette.colors.take(12).forEach { c ->
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { color = c },
                        shape = CircleShape,
                        color = Color(c),
                    ) {
                        if (color == c) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(5.dp),
                            )
                        }
                    }
                }
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val ids = when (scope) {
                        BudgetScope.ALL -> emptyList()
                        BudgetScope.SPECIFIC -> selectedCategoryId?.let { listOf(it) } ?: emptyList()
                        BudgetScope.MULTIPLE -> selectedIds.toList()
                    }
                    if (scope != BudgetScope.ALL && ids.isEmpty()) {
                        error = "Choose a category"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "Enter an amount greater than zero"
                        return@Button
                    }
                    val startMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    val endMillis = endDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                    onSave(
                        Budget(
                            id = budget?.id ?: 0,
                            name = name.trim(),
                            categoryId = if (ids.isEmpty()) null else ids.first(),
                            categoryIds = ids,
                            limitAmount = (amount * 100).toLong().coerceAtLeast(1),
                            spentAmount = budget?.spentAmount ?: 0,
                            period = period,
                            startDate = if (period == BudgetPeriod.CUSTOM) startMillis else budget?.startDate ?: System.currentTimeMillis(),
                            endDate = if (period == BudgetPeriod.CUSTOM) endMillis else budget?.endDate,
                            colorHex = String.format("#%06X", 0xFFFFFF and color),
                            alertAt = alertAt,
                            isActive = budget?.isActive ?: true,
                            createdAt = budget?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Budget", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showStartPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { startDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
    if (showEndPicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (endDate ?: LocalDate.now().plusMonths(1))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { endDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
}

@Composable
private fun DateChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

/** Compact digit keypad for amount entry. */
@Composable
private fun BudgetKeypad(display: String, onInput: (String) -> Unit) {
    fun press(key: String) {
        when (key) {
            "⌫" -> onInput(display.dropLast(1))
            "." -> if ("." !in display) onInput(display + ".")
            else -> onInput(if (display == "0") key else display + key)
        }
    }
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { press(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = AmountFontFamily,
                        )
                    }
                }
            }
        }
    }
}
