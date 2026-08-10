package com.myexpense.tracker.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.R
import com.myexpense.tracker.ui.theme.InterFamily
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val gradient: List<Color>,
    val illustrationRes: Int,
    val headline: String,
    val description: String,
)

private val onboardingPages = listOf(
    OnboardingPageData(
        gradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        illustrationRes = R.drawable.ic_onboard_1,
        headline = "Track Every Rupee",
        description = "Add your income and expenses in seconds. " +
            "Know exactly where your money is going every single day.",
    ),
    OnboardingPageData(
        gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
        illustrationRes = R.drawable.ic_onboard_2,
        headline = "Smart Budgets & Goals",
        description = "Set budgets for every category. Create savings " +
            "goals and watch your dreams become reality step by step.",
    ),
    OnboardingPageData(
        gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
        illustrationRes = R.drawable.ic_onboard_3,
        headline = "Powerful Insights",
        description = "Beautiful charts and reports reveal your " +
            "spending patterns. Make smarter decisions with real data.",
    ),
    OnboardingPageData(
        gradient = listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
        illustrationRes = R.drawable.ic_onboard_4,
        headline = "100% Private & Offline",
        description = "Your data never leaves your phone. No cloud, " +
            "no account, no internet needed. Ever. Your money, your rules.",
    ),
)

/** 4-step onboarding with a HorizontalPager and a final setup bottom sheet. */
@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    var showSetup by remember { mutableStateOf(false) }
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageDots(
                current = pagerState.currentPage,
                count = onboardingPages.size,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isLastPage) {
                        showSetup = true
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A1035),
                ),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Text(
                    text = if (isLastPage) "Get Started 🚀" else "Next →",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }

    if (showSetup) {
        SetupBottomSheet(
            onDismiss = { showSetup = false },
            onDone = { currency, accountName, accountType, balanceMinor, biometric ->
                viewModel.completeSetup(
                    currency = currency,
                    accountName = accountName,
                    accountType = accountType,
                    balanceMinor = balanceMinor,
                    biometricEnabled = biometric,
                    onDone = onFinished,
                )
            },
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPageData) {
    val transition = rememberInfiniteTransition(label = "onboardingFloat")
    val floatY by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(page.gradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(page.illustrationRes),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer { translationY = floatY },
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = page.headline,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = page.description,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 22.dp else 8.dp,
                animationSpec = tween(200),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .size(width, 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index == current) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.4f)
                        }
                    ),
            )
        }
    }
}
