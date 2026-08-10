package com.myexpense.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myexpense.tracker.data.database.entity.AchievementEntity
import com.myexpense.tracker.ui.theme.AmountFontFamily
import com.myexpense.tracker.ui.theme.Elevation
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.theme.Spacing
import com.myexpense.tracker.ui.theme.TypeScale
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.IconMap
import com.myexpense.tracker.utils.MoneyFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

private val Gold = Color(0xFFD4AF37)

// ─────────────────────────────────────────────────────────────────────────────
// 1. AmountText — colored by income/expense, formatted, count-up animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AmountText(
    amountMinor: Long,
    symbol: String,
    isExpense: Boolean = true,
    style: TextStyle = TypeScale.H4,
    countUp: Boolean = true,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    val color = if (isExpense) MaterialTheme.colorScheme.onSurface else IncomeGreenForTheme()

    if (!countUp) {
        Text(
            text = (if (isExpense) "-" else "+") + "$symbol" + MoneyFormatter.format(amountMinor),
            style = style,
            fontFamily = AmountFontFamily,
            color = color,
            maxLines = maxLines,
            modifier = modifier,
        )
        return
    }

    val animated = remember { Animatable(amountMinor.toFloat()) }
    LaunchedEffect(amountMinor) {
        animated.animateTo(amountMinor.toFloat(), tween(600, easing = FastOutSlowInEasing))
    }
    val display = animated.value.toLong()
    Text(
        text = (if (isExpense) "-" else "+") + "$symbol" + MoneyFormatter.format(display),
        style = style,
        fontFamily = AmountFontFamily,
        color = color,
        maxLines = maxLines,
        modifier = modifier,
    )
}

@Composable
private fun IncomeGreenForTheme(): Color =
    if (MaterialTheme.colorScheme.primary == Color(0xFF6C63FF)) {
        com.myexpense.tracker.ui.theme.IncomeGreen
    } else {
        MaterialTheme.colorScheme.primary
    }

// ─────────────────────────────────────────────────────────────────────────────
// 2. TransactionListItem — standard row with swipe actions
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TransactionListItem(
    title: String,
    subtitle: String,
    icon: String?,
    iconColor: Color,
    amount: Long,
    symbol: String,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    if (onEdit == null && onDelete == null) {
        TransactionRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconColor = iconColor,
            amount = amount,
            symbol = symbol,
            isExpense = isExpense,
            modifier = modifier,
            onClick = onClick,
        )
        return
    }

    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> { onEdit?.invoke(); false }
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> { onDelete?.invoke(); false }
                else -> false
            }
        }
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onEdit != null,
        enableDismissFromEndToStart = onDelete != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2979FF)
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF1744)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg),
                contentAlignment = when (direction) {
                    androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                },
            ) {
                Icon(
                    imageVector = if (direction == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd) {
                        androidx.compose.material.icons.filled.Edit
                    } else {
                        androidx.compose.material.icons.filled.Delete
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(24.dp),
                )
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionRow(
                title = title,
                subtitle = subtitle,
                icon = icon,
                iconColor = iconColor,
                amount = amount,
                symbol = symbol,
                isExpense = isExpense,
                modifier = modifier,
                onClick = onClick,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. ProgressCard — title + progress bar + amounts
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressCard(
    title: String,
    spent: Long,
    limit: Long,
    symbol: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
) {
    val progress = if (limit > 0) (spent.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(progress, tween(500, easing = androidx.compose.animation.core.EaseInOut))
    }
    val barColor = when {
        spent > limit -> Color(0xFFB71C1C)
        progress > 0.8f -> Color(0xFFFF1744)
        progress > 0.6f -> Color(0xFFFF9800)
        else -> color
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Elevation.Cards,
    ) {
        Column(modifier = Modifier.padding(Spacing.Card)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = TypeScale.H4,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$symbol${MoneyFormatter.format(spent)} / $symbol${MoneyFormatter.format(limit)}",
                    style = TypeScale.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = TypeScale.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.Compact))
            LinearProgressIndicator(
                progress = { animated.value },
                modifier = Modifier.fillMaxWidth(),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. SummaryCard — gradient card with stats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(
        Color(0xFF6C63FF),
        Color(0xFF5A52E0),
    ),
    valueColor: Color = Color.White,
    subtitle: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = Color.Transparent,
        shadowElevation = Elevation.Cards,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(Spacing.Card),
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = TypeScale.Caption,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = value,
                    style = TypeScale.H2,
                    fontFamily = AmountFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = TypeScale.Caption,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. LoadingShimmer — skeleton shimmer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(x - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(x, 200f),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

@Composable
fun ShimmerListRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenHorizontal, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Item),
    ) {
        LoadingShimmer(modifier = Modifier.size(44.dp), cornerRadius = 22.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LoadingShimmer(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
            LoadingShimmer(modifier = Modifier.fillMaxWidth(0.4f).height(11.dp))
        }
        LoadingShimmer(modifier = Modifier.width(64.dp).height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. ConfirmDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "OK",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = com.myexpense.tracker.ui.theme.Radii.Dialog,
        title = { Text(title, style = TypeScale.H3) },
        text = { Text(message, style = TypeScale.Body2) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. DateRangePicker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateRangePicker(
    initialFrom: java.time.LocalDate,
    initialTo: java.time.LocalDate,
    onRangeSelected: (java.time.LocalDate, java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember { androidx.compose.runtime.mutableStateOf(initialFrom) }
    var to by remember { androidx.compose.runtime.mutableStateOf(initialTo) }
    var picking by remember { androidx.compose.runtime.mutableStateOf(true) } // true=from

    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = (if (picking) from else to)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = com.myexpense.tracker.ui.theme.Radii.Dialog,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    if (picking) {
                        from = date
                        picking = false
                    } else {
                        to = date
                        onRangeSelected(minOf(from, date), maxOf(from, date))
                    }
                }
            }) {
                Text(if (picking) "Next" else "Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        androidx.compose.material3.DatePicker(
            state = state,
            title = { Text(if (picking) "Start date" else "End date", style = TypeScale.H4) },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. CurrencyInput — field with currency prefix + built-in calculator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CurrencyInput(
    value: String,
    onValueChange: (String) -> Unit,
    symbol: String,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    showKeypad: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = { Text(symbol) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TypeScale.H2.copy(fontFamily = AmountFontFamily),
    )
    if (showKeypad) {
        Spacer(Modifier.height(Spacing.Compact))
        CompactCalculator(value = value, onInput = onValueChange)
    }
}

/** Compact digit keypad (also used by the budget sheet). */
@Composable
fun CompactCalculator(value: String, onInput: (String) -> Unit) {
    fun press(key: String) {
        when (key) {
            "⌫" -> onInput(value.dropLast(1))
            "." -> if ("." !in value) onInput(value + ".")
            else -> onInput(if (value == "0" || value.isEmpty()) key else value + key)
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
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { press(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = TypeScale.H4,
                            fontFamily = AmountFontFamily,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. IconPickerGrid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IconPickerGrid(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(IconMap.all.entries.toList()) { (key, vector) ->
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onSelect(key) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected == key) {
                    tint.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = vector,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (selected == key) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 13. ColorPickerRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColorPickerRow(
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = com.myexpense.tracker.ui.theme.CategoryColorLongs,
    showCustom: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { c ->
            Surface(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onSelect(c) },
                shape = CircleShape,
                color = Color(c),
            ) {
                if (selected == c) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
        if (showCustom) {
            Surface(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onSelect((0xFF000000 or (0..0xFFFFFF).random().toLong())) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎲", fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 14. SnackBarMessage — success / error / warning snackbar with icon
// ─────────────────────────────────────────────────────────────────────────────

enum class SnackType { SUCCESS, ERROR, WARNING, INFO }

@Composable
fun SnackBarMessage(
    hostState: SnackbarHostState,
    message: String,
    type: SnackType = SnackType.INFO,
) {
    val color = when (type) {
        SnackType.SUCCESS -> Color(0xFF43A047)
        SnackType.ERROR -> MaterialTheme.colorScheme.error
        SnackType.WARNING -> Color(0xFFFF9800)
        SnackType.INFO -> MaterialTheme.colorScheme.inverseSurface
    }
    val icon = when (type) {
        SnackType.SUCCESS -> Icons.Filled.Check
        SnackType.ERROR -> Icons.Filled.Error
        SnackType.WARNING -> Icons.Filled.Warning
        SnackType.INFO -> Icons.Filled.Info
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(message, type) {
        if (message.isNotEmpty()) {
            scope.launch {
                hostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 15. AchievementCard — locked / unlocked states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AchievementCard(
    achievement: AchievementEntity,
    modifier: Modifier = Modifier,
    isRecent: Boolean = false,
    consistentDaysLeft: Int? = null,
) {
    val unlocked = achievement.isUnlocked

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (unlocked) 1f else 0.55f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = if (unlocked) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = if (unlocked) Elevation.Cards else 0.dp,
            border = if (unlocked) {
                androidx.compose.foundation.BorderStroke(1.5.dp, Gold)
            } else null,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = if (unlocked) {
                                Brush.linearGradient(listOf(Color(0xFFFFD700), Gold))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)))
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconMap.get(achievement.iconName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = achievement.title,
                    style = TypeScale.H4,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (unlocked) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = achievement.description,
                    style = TypeScale.Caption,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (unlocked) {
                    achievement.unlockedAt?.let {
                        Text(
                            text = "Unlocked ${DateUtils.mediumDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}",
                            style = TypeScale.Caption,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        text = consistentDaysLeft?.let { "Days left: $it" } ?: "🔒",
                        style = TypeScale.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    androidx.compose.material.icons.filled.Lock,
                    contentDescription = "Locked",
                    tint = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (isRecent) {
            ConfettiBurst(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val colors = listOf(Color(0xFFFFD700), Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A))
    val seeds = remember { List(16) { it * 97 } }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        seeds.forEachIndexed { index, seed ->
            val x = ((seed % 89) / 89f) * size.width
            val fall = ((phase * 1.3f + (index % 4) * 0.25f) % 1.3f) / 1.3f
            val y = fall * size.height
            val sway = kotlin.math.sin(phase * 5f + seed) * 5f
            rotate(degrees = phase * 400f + seed, pivot = androidx.compose.ui.geometry.Offset(x + sway, y)) {
                drawRect(
                    color = colors[index % colors.size],
                    topLeft = androidx.compose.ui.geometry.Offset(x + sway, y),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 7.dp.toPx()),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animations — staggered list fade-in
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StaggeredFadeIn(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 50).toLong())
        launch { alpha.animateTo(1f, tween(250)) }
        launch { offsetY.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            },
    ) {
        content()
    }
}
