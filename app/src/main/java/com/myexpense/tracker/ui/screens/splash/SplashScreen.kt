package com.myexpense.tracker.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.R
import com.myexpense.tracker.ui.theme.InterFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash: deep purple→blue gradient, wallet icon scaling+fading in,
 * app name + tagline, pulsing dots at the bottom. Runs for 2.5 s and then
 * hands off to onboarding (first launch) or home (returning user).
 *
 * The Android 12+ system splash (Theme.MoneyMate.Starting) shows first; this
 * composable takes over seamlessly once the first frame is drawn.
 */
@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(500)) }
        launch { scale.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1035), Color(0xFF0D1B4B))
                )
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_splash_wallet),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "MoneyMate",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your Smart Money Companion",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        LoadingDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
        )
    }
}

/** Three pulsing dots, staggered so they ripple. */
@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PulsingDot(delayMs = 0)
        PulsingDot(delayMs = 200)
        PulsingDot(delayMs = 400)
    }
}

@Composable
private fun PulsingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "splashDots")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "dotAlpha",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = dotAlpha))
            .scale(0.7f + dotAlpha * 0.3f),
    )
}
