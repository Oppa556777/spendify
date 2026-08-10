package com.myexpense.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.R

/** Primary font: Nunito (rounded, friendly). */
val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_light, FontWeight.Light),
    Font(R.font.nunito_extralight, FontWeight.ExtraLight),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** Secondary font: Inter (clean, great for numbers). */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val defaultTypography = Typography()

val MoneyMateTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.Bold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = NunitoFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = NunitoFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = NunitoFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = InterFamily, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

/** Font family used for amounts / numeric read-outs. */
val AmountFontFamily = InterFamily

/** Scales every text style by [factor] (font size presets). */
fun scaleTypography(base: Typography, factor: Float): Typography = Typography(
    displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * factor),
    displayMedium = base.displayMedium.copy(fontSize = base.displayMedium.fontSize * factor),
    displaySmall = base.displaySmall.copy(fontSize = base.displaySmall.fontSize * factor),
    headlineLarge = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize * factor),
    headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * factor),
    headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * factor),
    titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * factor),
    titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * factor),
    titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * factor),
    bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * factor),
    bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * factor),
    bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * factor),
    labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * factor),
    labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * factor),
    labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * factor),
)
