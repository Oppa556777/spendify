package com.myexpense.tracker.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating pill-shaped bottom navigation bar with an elevated center "+" button.
 * Tapping the center button opens the animated speed dial (Expense/Income/Transfer).
 */
@Composable
fun FloatingPillNavBar(
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillNavItem(
                    label = "Home",
                    icon = Icons.Filled.Home,
                    selected = selectedRoute == Routes.HOME,
                    onClick = { onSelect(Routes.HOME) },
                )
                PillNavItem(
                    label = "Accounts",
                    icon = Icons.Filled.AccountBalanceWallet,
                    selected = selectedRoute == Routes.ACCOUNTS,
                    onClick = { onSelect(Routes.ACCOUNTS) },
                )
                // Center add button, raised above the bar.
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-18).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3B82F6))))
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                PillNavItem(
                    label = "Reports",
                    icon = Icons.Filled.BarChart,
                    selected = selectedRoute == Routes.STATS,
                    onClick = { onSelect(Routes.STATS) },
                )
                PillNavItem(
                    label = "More",
                    icon = Icons.Filled.MoreVert,
                    selected = selectedRoute == Routes.MORE,
                    onClick = { onSelect(Routes.MORE) },
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Dimmed scrim + three options that fan out above the center add button. */
@Composable
fun SpeedDialOverlay(
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onDismiss),
        )

        // options anchored above the nav bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(240.dp, 120.dp)) {
                SpeedDialOption(
                    icon = Icons.Filled.ArrowDownward,
                    label = "Expense",
                    color = Color(0xFFE53935),
                    x = 8.dp,
                    y = 0.dp,
                    onClick = onExpense,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
                SpeedDialOption(
                    icon = Icons.Filled.ArrowUpward,
                    label = "Income",
                    color = Color(0xFF43A047),
                    x = 82.dp,
                    y = (-34).dp,
                    onClick = onIncome,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                SpeedDialOption(
                    icon = Icons.Filled.SwapHoriz,
                    label = "Transfer",
                    color = Color(0xFF3B82F6),
                    x = 8.dp,
                    y = 0.dp,
                    onClick = onTransfer,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun SpeedDialOption(
    icon: ImageVector,
    label: String,
    color: Color,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "speedDialScale",
    )
    Column(
        modifier = modifier.offset(x = x, y = y),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
