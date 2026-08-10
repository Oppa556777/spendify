package com.myexpense.tracker.ui.screens.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.matchParentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.model.Goal
import com.myexpense.tracker.data.model.GoalWithProgress
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.components.CategoryIcon
import com.myexpense.tracker.ui.components.EmptyState
import com.myexpense.tracker.ui.screens.accounts.Palette
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.GoalsViewModel
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.IconMap
import com.myexpense.tracker.utils.MoneyFormatter
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val symbol = state.currencySymbol
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var addMoneyGoal by remember { mutableStateOf<GoalWithProgress?>(null) }
    var pendingDelete by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Savings Goals", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Filled.Add, contentDescription = "Add goal", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.goals.isEmpty()) {
            EmptyState(
                title = "No savings goals yet",
                subtitle = "Create a goal and watch your savings grow.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.goals, key = { it.goal.id }) { goalWithProgress ->
                    GoalCard(
                        goalWithProgress = goalWithProgress,
                        symbol = symbol,
                        onAddMoney = { addMoneyGoal = goalWithProgress },
                        onClick = {
                            editing = goalWithProgress.goal
                            showSheet = true
                        },
                        onDelete = { pendingDelete = goalWithProgress.goal },
                    )
                }
            }
        }
    }

    // ── Add money dialog ───────────────────────────────────────────────────
    addMoneyGoal?.let { gwp ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addMoneyGoal = null },
            title = { Text("Add money to \"${gwp.goal.name}\"") },
            text = {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text(symbol) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value > 0) {
                        viewModel.addMoney(gwp.goal.id, (value * 100).toLong())
                        addMoneyGoal = null
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { addMoneyGoal = null }) { Text("Cancel") }
            },
        )
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    pendingDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete goal?") },
            text = { Text("Delete \"${goal.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(goal.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // ── Add/Edit sheet ─────────────────────────────────────────────────────
    if (showSheet) {
        AddEditGoalSheet(
            goal = editing,
            accounts = state.accounts,
            symbol = symbol,
            onDismiss = { showSheet = false },
            onSave = { goal ->
                viewModel.save(goal)
                showSheet = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GOAL CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goalWithProgress: GoalWithProgress,
    symbol: String,
    onAddMoney: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val goal = goalWithProgress.goal
    val base = Color(goal.color)
    val gradient = remember(goal.color) {
        listOf(base, blend(base, 0.35f))
    }
    val complete = goal.isCompleted || goalWithProgress.progress >= 1f

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradient))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = IconMap.get(goal.iconName),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = goal.name,
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        goal.note?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    GoalProgressRing(
                        progress = goalWithProgress.progress,
                        modifier = Modifier.size(64.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { goalWithProgress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$symbol${MoneyFormatter.format(goal.savedAmount)} / $symbol${MoneyFormatter.format(goal.targetAmount)}",
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (complete) {
                            "Goal completed! 🎉"
                        } else {
                            "$symbol${MoneyFormatter.format(goalWithProgress.remaining)} more to go"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = goal.deadline?.let {
                            "🎯 ${YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()).let { ym -> DateUtils.monthYear(ym) }}"
                        } ?: "No deadline",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAddMoney,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = base),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("+ Add Money", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(0.4f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (complete) {
            ConfettiOverlay(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun GoalProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) { animated.snapTo(0f); animated.animateTo(progress, tween(700)) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.10f
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            color = Color.White,
            fontFamily = AmountFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiPhase",
    )
    val colors = listOf(Color(0xFFFFD54F), Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC))
    val seeds = remember { List(24) { it * 137 } }

    Canvas(modifier = modifier) {
        seeds.forEachIndexed { index, seed ->
            val x = ((seed % 97) / 97f) * size.width
            val fall = ((phase * 1.4f + (index % 5) * 0.2f) % 1.4f) / 1.4f
            val y = fall * size.height
            val sway = kotlin.math.sin(phase * 6f + seed) * 6f
            rotate(degrees = phase * 360f + seed, pivot = Offset(x + sway, y)) {
                drawRect(
                    color = colors[index % colors.size],
                    topLeft = Offset(x + sway, y),
                    size = androidx.compose.ui.geometry.Size(5.dp.toPx(), 8.dp.toPx()),
                )
            }
        }
    }
}

private fun blend(color: Color, factor: Float): Color = Color(
    red = color.red * (1f - factor),
    green = color.green * (1f - factor),
    blue = color.blue * (1f - factor),
)

// ─────────────────────────────────────────────────────────────────────────────
// ADD / EDIT GOAL SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditGoalSheet(
    goal: Goal?,
    accounts: List<com.myexpense.tracker.data.model.Account>,
    symbol: String,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(goal?.let { MoneyFormatter.format(it.targetAmount) } ?: "") }
    var initial by remember { mutableStateOf(goal?.let { MoneyFormatter.format(it.savedAmount) } ?: "") }
    var deadline by remember { mutableStateOf(goal?.deadline) }
    var icon by remember { mutableStateOf(goal?.iconName ?: "star") }
    var color by remember { mutableStateOf(goal?.color ?: 0xFF6C63FF) }
    var accountId by remember { mutableStateOf(goal?.accountId) }
    var note by remember { mutableStateOf(goal?.note ?: "") }
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
                text = if (goal == null) "New Goal" else "Edit Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal name") },
                placeholder = { Text("e.g. New Bike, Goa Trip") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Target amount") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = initial,
                onValueChange = { initial = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Initial savings") },
                prefix = { Text(symbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deadline?.let {
                        "🎯 ${DateUtils.mediumDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}"
                    } ?: "No deadline",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    // toggle: set to 1 year out or clear
                    deadline = if (deadline == null) System.currentTimeMillis() + 365L * 86_400_000L else null
                }) {
                    Text(if (deadline == null) "Set deadline" else "Clear")
                }
            }

            Text("Icon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("star", "savings", "flight", "home", "school", "celebration", "account_balance", "diamond").forEach { key ->
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { icon = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (icon == key) Color(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = IconMap.get(key),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (icon == key) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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

            Text("Linked account (optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.clickable { accountId = null },
                    shape = RoundedCornerShape(12.dp),
                    color = if (accountId == null) Color(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        "None",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
                accounts.forEach { account ->
                    val selected = accountId == account.id
                    Surface(
                        modifier = Modifier.clickable { accountId = account.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Color(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
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
                    if (name.isBlank()) { error = "Enter a goal name"; return@Button }
                    val targetAmount = target.toDoubleOrNull() ?: 0.0
                    if (targetAmount <= 0) { error = "Enter a target amount"; return@Button }
                    val initialAmount = initial.toDoubleOrNull() ?: 0.0
                    onSave(
                        Goal(
                            id = goal?.id ?: 0,
                            name = name.trim(),
                            targetAmount = (targetAmount * 100).toLong().coerceAtLeast(1),
                            savedAmount = (initialAmount * 100).toLong(),
                            deadline = deadline,
                            iconName = icon,
                            color = color,
                            accountId = accountId,
                            note = note.trim().ifBlank { null },
                            isCompleted = goal?.isCompleted ?: false,
                            createdAt = goal?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Save Goal", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}
