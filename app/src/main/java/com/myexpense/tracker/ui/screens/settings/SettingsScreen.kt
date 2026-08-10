package com.myexpense.tracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Accent
import com.myexpense.tracker.data.model.DateFormat
import com.myexpense.tracker.data.model.FontSize
import com.myexpense.tracker.data.model.LockDelay
import com.myexpense.tracker.data.model.NumberFormat
import com.myexpense.tracker.data.model.ThemeMode
import com.myexpense.tracker.data.model.WeekStart
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.ClearDataViewModel
import com.myexpense.tracker.ui.viewmodel.SettingsViewModel
import com.myexpense.tracker.utils.AllCurrencies

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenLoans: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenBackup: () -> Unit,
    onClearAll: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    clearDataViewModel: ClearDataViewModel = hiltViewModel(),
) {
    val settings = viewModel.settings
    var showClearConfirm by remember { mutableStateOf(false) }
    var showClearConfirm2 by remember { mutableStateOf(false) }
    var dailyTimeDialog by remember { mutableStateOf(false) }
    var dailyTimeInput by remember { mutableStateOf(settings.dailyReminderTime) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("App Settings", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── APPEARANCE ─────────────────────────────────────────────────
            item { SectionLabel("Appearance") }
            item {
                SettingsCard {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.AMOLED to "AMOLED",
                        )
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Dynamic colors (Android 12+)", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = settings.dynamicColors, onCheckedChange = viewModel::setDynamicColors)
                }
            }
            item {
                SettingsCard {
                    Text("App color", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Accent.entries.forEach { accent ->
                            val selected = settings.accent == accent
                            Surface(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { viewModel.setAccent(accent) },
                                shape = CircleShape,
                                color = Color(accent.color),
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(7.dp),
                                    )
                                }
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Accent.entries.forEach { accent ->
                            Text(
                                text = accent.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.accent == accent) {
                                    Color(accent.color)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (settings.accent == accent) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Font size", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Small", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.fontSize.ordinal.toFloat(),
                            onValueChange = {
                                viewModel.setFontSize(FontSize.entries[it.toInt().coerceIn(0, 2)])
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text("Large", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "Current: ${settings.fontSize.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── CURRENCY & LOCALE ──────────────────────────────────────────
            item { SectionLabel("Currency & Locale") }
            item {
                SettingsCard {
                    Text("Default currency", style = MaterialTheme.typography.titleSmall)
                    val current = AllCurrencies.firstOrNull { it.symbol == settings.currencySymbol }
                        ?: AllCurrencies.first()
                    Text(
                        text = "${current.symbol} ${current.code} — ${current.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AllCurrencies.take(20).forEach { info ->
                            val selected = settings.currencySymbol == info.symbol
                            Surface(
                                modifier = Modifier.clickable { viewModel.setCurrency(info.symbol) },
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = "${info.symbol} ${info.code}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Date format", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DateFormat.entries.forEachIndexed { index, format ->
                            SegmentedButton(
                                selected = settings.dateFormat == format,
                                onClick = { viewModel.setDateFormat(format) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = DateFormat.entries.size),
                                label = { Text(format.label) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Start of week", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        WeekStart.entries.forEachIndexed { index, start ->
                            SegmentedButton(
                                selected = settings.weekStart == start,
                                onClick = { viewModel.setWeekStart(start) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = WeekStart.entries.size),
                                label = { Text(start.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Start of month: day ${settings.monthStartDay}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.monthStartDay.toFloat(),
                        onValueChange = { viewModel.setMonthStartDay(it.toInt()) },
                        valueRange = 1f..31f,
                    )
                }
            }
            item {
                SettingsCard {
                    Text("Number format", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        NumberFormat.entries.forEachIndexed { index, format ->
                            SegmentedButton(
                                selected = settings.numberFormat == format,
                                onClick = { viewModel.setNumberFormat(format) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = NumberFormat.entries.size),
                                label = { Text(format.label) },
                            )
                        }
                    }
                }
            }

            // ── SECURITY ───────────────────────────────────────────────────
            item { SectionLabel("Security") }
            item {
                SettingsCard {
                    ToggleRow("App Lock", "Lock the app with fingerprint / PIN", settings.biometricEnabled, viewModel::setBiometricEnabled)
                }
            }
            item {
                SettingsCard {
                    ToggleRow("Biometric authentication", "Use fingerprint / face to unlock", settings.biometricEnabled, viewModel::setBiometricEnabled)
                }
            }
            item {
                SettingsCard {
                    Text("Lock after", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LockDelay.entries.forEach { delay ->
                            Surface(
                                modifier = Modifier.clickable { viewModel.setLockDelay(delay) },
                                shape = RoundedCornerShape(50),
                                color = if (settings.lockDelay == delay) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ) {
                                Text(
                                    text = delay.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (settings.lockDelay == delay) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsCard {
                    ToggleRow("Hide balance by default", "Show blurred until tapped", settings.hideBalanceDefault, viewModel::setHideBalanceDefault)
                }
            }

            // ── NOTIFICATIONS ──────────────────────────────────────────────
            item { SectionLabel("Notifications") }
            item {
                SettingsCard {
                    ToggleRow("Budget alerts", "Notify when a budget passes its alert %", settings.notifyBudget, viewModel::setNotifyBudget)
                    ToggleRow("Bill reminders", "Remind about upcoming split bills", settings.notifyBill, viewModel::setNotifyBill)
                    ToggleRow("Subscription reminders", "Remind before subscriptions renew", settings.notifySubscriptions, viewModel::setNotifySubscriptions)
                    ToggleRow("Loan due reminders", "Remind about overdue loans", settings.notifyLoans, viewModel::setNotifyLoans)
                    ToggleRow("Achievement notifications", "Notify when an achievement unlocks", settings.notifyAchievements, viewModel::setNotifyAchievements)
                    ToggleRow("Daily reminder to log expenses", "Remind you to log the day's expenses", settings.notifyDaily, viewModel::setNotifyDaily)
                    if (settings.notifyDaily) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Reminder time: ${settings.dailyReminderTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                dailyTimeInput = settings.dailyReminderTime
                                dailyTimeDialog = true
                            }) {
                                Text("Set")
                            }
                        }
                    }
                }
            }

            // ── CATEGORIES ─────────────────────────────────────────────────
            item { SectionLabel("Categories") }
            item {
                NavRow(Icons.Filled.Category, "Manage Categories", "Edit, add, or delete categories", onOpenCategories)
            }

            // ── DATA MANAGEMENT ────────────────────────────────────────────
            item { SectionLabel("Data Management") }
            item {
                NavRow(Icons.Filled.Backup, "Backup Data", "Encrypted ZIP backup to a chosen folder", onOpenBackup)
            }
            item {
                NavRow(Icons.Filled.Restore, "Restore Data", "Import from a ZIP backup", onOpenBackup)
            }
            item {
                NavRow(Icons.Filled.Description, "Export PDF", "Full financial report", onOpenBackup)
            }
            item {
                NavRow(Icons.Filled.TableChart, "Export CSV", "All transactions as a spreadsheet", onOpenBackup)
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirm = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "  Clear All Data",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "Wipes everything on this device. 2-step confirmation.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Clear-all two-step confirmation ────────────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("This will permanently delete all transactions, accounts, budgets, goals and settings on this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    showClearConfirm2 = true
                }) { Text("Continue", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
    if (showClearConfirm2) {
        AlertDialog(
            onDismissRequest = { showClearConfirm2 = false },
            title = { Text("Are you absolutely sure?") },
            text = { Text("Final confirmation: all your data will be erased from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm2 = false
                    clearDataViewModel.clearAll()
                    onClearAll()
                }) { Text("Erase everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm2 = false }) { Text("Cancel") }
            },
        )
    }

    // ── Daily reminder time dialog ─────────────────────────────────────────
    if (dailyTimeDialog) {
        AlertDialog(
            onDismissRequest = { dailyTimeDialog = false },
            title = { Text("Daily reminder time") },
            text = {
                OutlinedTextField(
                    value = dailyTimeInput,
                    onValueChange = { dailyTimeInput = it },
                    label = { Text("HH:mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dailyTimeInput.matches(Regex("\\d{2}:\\d{2}"))) {
                        viewModel.setDailyReminderTime(dailyTimeInput)
                        dailyTimeDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { dailyTimeDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable Column.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
