package com.myexpense.tracker.ui.screens.achievements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.data.database.entity.AchievementEntity
import com.myexpense.tracker.ui.components.BackButton
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.AchievementsViewModel
import com.myexpense.tracker.utils.DateUtils
import java.time.Instant
import java.time.ZoneOffset

private val Gold = Color(0xFFD4AF37)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Achievements", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${state.unlockedCount} / ${state.totalCount} unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.achievements, key = { it.id }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isRecent = achievement.id in state.recentlyUnlockedIds,
                    consistentDaysLeft = state.consistentDaysLeft,
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: AchievementEntity,
    isRecent: Boolean,
    consistentDaysLeft: Int?,
) {
    val unlocked = achievement.isUnlocked
    val iconColor = if (unlocked) Gold else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (unlocked) 1f else 0.55f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (unlocked) {
                    Color(0xFFFFF8E1)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 3.dp else 0.dp),
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
                                Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFD4AF37)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)))
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = com.myexpense.tracker.utils.IconMap.get(achievement.iconName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (unlocked) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (unlocked) {
                    achievement.unlockedAt?.let {
                        Text(
                            text = "Unlocked ${DateUtils.mediumDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        text = consistentDaysLeft?.let { "Days left: $it" } ?: "🔒",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Locked overlay
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Confetti burst for recently unlocked
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

    Canvas(modifier = modifier) {
        seeds.forEachIndexed { index, seed ->
            val x = ((seed % 89) / 89f) * size.width
            val fall = ((phase * 1.3f + (index % 4) * 0.25f) % 1.3f) / 1.3f
            val y = fall * size.height
            val sway = kotlin.math.sin(phase * 5f + seed) * 5f
            rotate(degrees = phase * 400f + seed, pivot = Offset(x + sway, y)) {
                drawRect(
                    color = colors[index % colors.size],
                    topLeft = Offset(x + sway, y),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 7.dp.toPx()),
                )
            }
        }
    }
}
