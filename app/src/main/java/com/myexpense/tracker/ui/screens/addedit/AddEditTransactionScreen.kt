package com.myexpense.tracker.ui.screens.addedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.myexpense.tracker.data.model.RecurringFrequency
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.AddEditTransactionViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    onSplitBill: (() -> Unit)? = null,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var accountPickerTarget by remember { mutableStateOf(AccountPickerTarget.FROM) }
    var showPersonPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showReceiptChooser by remember { mutableStateOf(false) }
    var showRecurringEndPicker by remember { mutableStateOf(false) }
    var showQuickCategoryDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showSuccessOverlay by remember { mutableStateOf(false) }

    // ── Receipt capture ────────────────────────────────────────────────────
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.setReceipt(it.toString()) } }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { viewModel.setReceipt(it.toString()) } }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showSuccessOverlay = true
            delay(500)
            onDone()
        }
    }

    val typeGradient = when (state.type) {
        TransactionType.EXPENSE -> listOf(Color(0xFFFF6B6B), Color(0xFFC62828))
        TransactionType.INCOME -> listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
        TransactionType.TRANSFER -> listOf(Color(0xFF64B5F6), Color(0xFF1E40AF))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim (tap outside dismisses)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDone() },
        )

        // Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.98f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                // Tabs
                SheetTabs(
                    selected = state.type,
                    onSelect = viewModel::setType,
                    onClose = onDone,
                )

                // ── Amount + calculator ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(typeGradient))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "${state.currencySymbol} ${displayAmount(state.calc.display)}",
                        fontFamily = AmountFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CalculatorKeypad(
                        display = state.calc.display,
                        onDigit = viewModel::digit,
                        onDecimal = viewModel::decimalPoint,
                        onBackspace = viewModel::backspace,
                        onOperator = viewModel::pressOperator,
                        onSave = viewModel::save,
                    )
                }

                // ── Details ─────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 1. Title + smart suggestions
                    var titleFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { titleFocused = it.isFocused },
                        placeholder = { Text("What did you spend on?") },
                        singleLine = true,
                    )
                    if (titleFocused && state.title.isNotBlank()) {
                        val suggestions = state.suggestions
                            .filter { it.contains(state.title, ignoreCase = true) && it != state.title }
                            .take(4)
                        suggestions.forEach { suggestion ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTitle(suggestion) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }

                    // 2. Category (or transfer accounts)
                    if (state.type == TransactionType.TRANSFER) {
                        TransferAccountsSection(
                            accounts = state.accounts,
                            symbol = state.currencySymbol,
                            fromId = state.accountId,
                            toId = state.toAccountId,
                            onFrom = {
                                accountPickerTarget = AccountPickerTarget.FROM
                                showAccountPicker = true
                            },
                            onTo = {
                                accountPickerTarget = AccountPickerTarget.TO
                                showAccountPicker = true
                            },
                            onSwap = {
                                scope.launch {
                                    val from = state.accountId
                                    viewModel.setAccountId(state.toAccountId)
                                    viewModel.setToAccountId(from)
                                }
                            },
                        )
                    } else {
                        val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }
                        DetailSelectRow(
                            icon = {
                                CategoryIcon(
                                    icon = selectedCategory?.icon,
                                    color = Color(selectedCategory?.color ?: 0xFF6C63FF),
                                    size = 36,
                                )
                            },
                            title = selectedCategory?.name ?: "Select category",
                            subtitle = "Required",
                            onClick = { showCategoryPicker = true },
                        )
                    }

                    // 3. Account
                    val selectedAccount = state.accounts.firstOrNull { it.id == state.accountId }
                    DetailSelectRow(
                        icon = {
                            CategoryIcon(icon = selectedAccount?.icon, color = Color(selectedAccount?.color ?: 0xFF3B82F6), size = 36)
                        },
                        title = selectedAccount?.name ?: "Select account",
                        subtitle = if (selectedAccount != null) {
                            "${state.currencySymbol}${MoneyFormatter.format(selectedAccount.balance)} balance"
                        } else {
                            "Required"
                        },
                        onClick = { showAccountPicker = true },
                    )

                    // 4. Date & time
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChipButton(
                            text = "📅 ${DateUtils.chipDate(state.date)}",
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                        ChipButton(
                            text = "🕐 ${DateUtils.timeLabel(state.time)}",
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // 5. Tags
                    Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    var addingTag by remember { mutableStateOf(false) }
                    var tagInput by remember { mutableStateOf("") }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.allTags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in state.tags,
                                onClick = { viewModel.toggleTag(tag.id) },
                                label = { Text(tag.name) },
                            )
                        }
                        if (!addingTag) {
                            FilterChip(
                                selected = false,
                                onClick = { addingTag = true },
                                label = { Text("+ Add") },
                            )
                        }
                    }
                    if (addingTag) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                placeholder = { Text("Tag name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    viewModel.createTag(tagInput)
                                    tagInput = ""
                                    addingTag = false
                                },
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add tag")
                            }
                        }
                    }

                    // 6. Person
                    val selectedPerson = state.people.firstOrNull { it.id == state.personId }
                    DetailSelectRow(
                        icon = {
                            CategoryIcon(icon = null, color = Color(selectedPerson?.avatarColor ?: 0xFF3B82F6), size = 36).let {
                                Box(contentAlignment = Alignment.Center) {
                                    it
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color(selectedPerson?.avatarColor ?: 0xFF3B82F6),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        title = selectedPerson?.name ?: "👤 Add Person",
                        subtitle = selectedPerson?.phone ?: "Optional — associate this entry with someone",
                        onClick = { showPersonPicker = true },
                    )

                    // 7. Location
                    DetailSelectRow(
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                CategoryIcon(icon = null, color = Color(0xFFFF7043), size = 36)
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFFF7043), modifier = Modifier.size(18.dp))
                            }
                        },
                        title = state.locationName ?: "📍 Add Location",
                        subtitle = "Optional — where did this happen?",
                        onClick = { showLocationDialog = true },
                    )

                    // 8. Note
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a note...") },
                        minLines = 2,
                        maxLines = 4,
                    )

                    // 9. Receipt
                    if (state.receiptPath == null) {
                        DetailSelectRow(
                            icon = {
                                Box(contentAlignment = Alignment.Center) {
                                    CategoryIcon(icon = null, color = Color(0xFF8E24AA), size = 36)
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF8E24AA), modifier = Modifier.size(18.dp))
                                }
                            },
                            title = "📷 Attach Receipt",
                            subtitle = "Camera or gallery — stored locally",
                            onClick = { showReceiptChooser = true },
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AsyncImage(
                                model = state.receiptPath,
                                contentDescription = "Receipt",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Text(
                                text = "Receipt attached",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.setReceipt(null) }) {
                                Text("Remove")
                            }
                        }
                    }

                    // 9b. Split bill shortcut
                    onSplitBill?.let { splitBill ->
                        DetailSelectRow(
                            icon = {
                                Box(contentAlignment = Alignment.Center) {
                                    CategoryIcon(icon = null, color = Color(0xFF6C63FF), size = 36)
                                    Icon(
                                        Icons.Filled.Group,
                                        contentDescription = null,
                                        tint = Color(0xFF6C63FF),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            title = "Split Bill",
                            subtitle = "Divide this expense with friends",
                            onClick = splitBill,
                        )
                    }

                    // 10. Recurring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "🔄 Make this recurring",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = state.isRecurring, onCheckedChange = viewModel::setRecurring)
                    }
                    if (state.isRecurring) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RecurringFrequency.entries.forEach { freq ->
                                FilterChip(
                                    selected = state.frequency == freq,
                                    onClick = { viewModel.setFrequency(freq) },
                                    label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = state.interval.toString(),
                                onValueChange = { viewModel.setInterval(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1) },
                                label = { Text("Repeat every") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(120.dp),
                            )
                            Text(
                                text = state.frequency.name.lowercase() + "s",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.recurringEnd?.let { "Ends ${DateUtils.mediumDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}" } ?: "No end date",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { showRecurringEndPicker = true }) {
                                Text("Set end")
                            }
                            if (state.recurringEnd != null) {
                                TextButton(onClick = { viewModel.setRecurringEnd(null) }) {
                                    Text("Clear")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        if (showSuccessOverlay) {
            SuccessOverlay()
        }

        // ── Nested pickers ─────────────────────────────────────────────────
        if (showCategoryPicker) {
            CategoryPickerOverlay(
                categories = state.categories.filter { it.type == state.type },
                selectedId = state.categoryId,
                onSelect = { id ->
                    viewModel.setCategoryId(id)
                    showCategoryPicker = false
                },
                onCreate = {
                    showCategoryPicker = false
                    showQuickCategoryDialog = true
                },
                onDismiss = { showCategoryPicker = false },
            )
        }
        if (showAccountPicker) {
            val isTransfer = state.type == TransactionType.TRANSFER
            val pickerTarget = accountPickerTarget
            AccountPickerOverlay(
                accounts = state.accounts,
                symbol = state.currencySymbol,
                selectedId = when {
                    !isTransfer -> state.accountId
                    pickerTarget == AccountPickerTarget.FROM -> state.accountId
                    else -> state.toAccountId
                },
                onSelect = { id ->
                    when {
                        !isTransfer -> viewModel.setAccountId(id)
                        pickerTarget == AccountPickerTarget.FROM -> viewModel.setAccountId(id)
                        else -> viewModel.setToAccountId(id)
                    }
                    showAccountPicker = false
                },
                onDismiss = { showAccountPicker = false },
            )
        }
        if (showPersonPicker) {
            PersonPickerOverlay(
                people = state.people,
                selectedId = state.personId,
                onSelect = { id ->
                    viewModel.setPersonId(id)
                    showPersonPicker = false
                },
                onCreate = {
                    showPersonPicker = false
                    showAddPersonDialog = true
                },
                onDismiss = { showPersonPicker = false },
            )
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
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

    if (showRecurringEndPicker) {
        val endState = rememberDatePickerState(
            initialSelectedDateMillis = (state.recurringEnd ?: System.currentTimeMillis())
                .let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
        )
        DatePickerDialog(
            onDismissRequest = { showRecurringEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endState.selectedDateMillis?.let { viewModel.setRecurringEnd(it) }
                    showRecurringEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringEndPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = endState)
        }
    }

    if (showTimePicker) {
        val parts = state.time.split(":").mapNotNull { it.toIntOrNull() }
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0) ?: 12,
            initialMinute = parts.getOrNull(1) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(String.format("%02d:%02d", timeState.hour, timeState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    if (showLocationDialog) {
        var locationText by remember { mutableStateOf(state.locationName ?: "") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location") },
            text = {
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    placeholder = { Text("e.g. Big Bazaar, Connaught Place") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setLocation(locationText)
                    showLocationDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showReceiptChooser) {
        AlertDialog(
            onDismissRequest = { showReceiptChooser = false },
            title = { Text("Attach receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showReceiptChooser = false
                        galleryLauncher.launch("image/*")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("📁  Choose from gallery")
                    }
                    TextButton(onClick = {
                        showReceiptChooser = false
                        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
                        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("📷  Take a photo")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReceiptChooser = false }) { Text("Cancel") }
            },
        )
    }

    if (showQuickCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickCategoryDialog = false },
            title = { Text("New category") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createCategory(name)
                    showQuickCategoryDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickCategoryDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Add person") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("Phone (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPerson(name, phone)
                    showAddPersonDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TABS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetTabs(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                TransactionType.EXPENSE to "EXPENSE",
                TransactionType.INCOME to "INCOME",
                TransactionType.TRANSFER to "TRANSFER",
            ).forEach { (type, label) ->
                val active = selected == type
                val color = when (type) {
                    TransactionType.EXPENSE -> Color(0xFFE53935)
                    TransactionType.INCOME -> Color(0xFF43A047)
                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                }
                Surface(
                    modifier = Modifier.clickable { onSelect(type) },
                    shape = RoundedCornerShape(50),
                    color = if (active) color else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CALCULATOR
// ─────────────────────────────────────────────────────────────────────────────

private val calculatorRows = listOf(
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf(".", "0", "⌫", "+"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorKeypad(
    display: String,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onOperator: (Char) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        calculatorRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    val isOperator = key in "÷×−+"
                    CalcKey(
                        label = key,
                        background = if (isOperator) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.35f),
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onLongClick = if (key == "⌫") {
                            { /* long-press clears */ }
                        } else null,
                    ) {
                        when {
                            key.isDigit() -> onDigit(key[0])
                            key == "." -> onDecimal()
                            key == "⌫" -> onBackspace()
                            else -> onOperator(key[0])
                        }
                    }
                }
            }
        }
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1A1035),
            ),
        ) {
            Text(
                text = "SAVE",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val clickModifier = if (onLongClick != null) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
    }
    Box(modifier = clickModifier.height(48.dp), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            color = textColor,
            fontSize = 22.sp,
            fontWeight = if (label in "÷×−+") FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = AmountFontFamily,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED DETAIL ROWS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailSelectRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRANSFER ACCOUNTS (animated arrow between From/To)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransferAccountsSection(
    accounts: List<com.myexpense.tracker.data.model.Account>,
    symbol: String,
    fromId: Long?,
    toId: Long?,
    onFrom: () -> Unit,
    onTo: () -> Unit,
    onSwap: () -> Unit,
) {
    val from = accounts.firstOrNull { it.id == fromId }
    val to = accounts.firstOrNull { it.id == toId }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFrom),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIcon(icon = from?.icon, color = Color(from?.color ?: 0xFF3B82F6), size = 36)
                Column(modifier = Modifier.weight(1f)) {
                    Text("From account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = from?.name ?: "Select account",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = from?.let { "$symbol${MoneyFormatter.format(it.balance)}" } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        scope.launch { rotation.animateTo(rotation.value + 180f, tween(300)) }
                        onSwap()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.SwapVert,
                    contentDescription = "Swap accounts",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTo),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIcon(icon = to?.icon, color = Color(to?.color ?: 0xFF43A047), size = 36)
                Column(modifier = Modifier.weight(1f)) {
                    Text("To account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = to?.name ?: "Select destination",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUCCESS OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF43A047),
            modifier = Modifier.size(84.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 44.sp, fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class AccountPickerTarget { FROM, TO }

// ─────────────────────────────────────────────────────────────────────────────
// AMOUNT DISPLAY FORMATTING
// ─────────────────────────────────────────────────────────────────────────────

private fun displayAmount(raw: String): String {
    if (raw == "0") return "0"
    val parts = raw.split(".")
    val grouped = MoneyFormatter.groupIndian(parts[0])
    return if (parts.size > 1) "$grouped.${parts[1]}" else grouped
}
