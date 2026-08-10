package com.myexpense.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myexpense.tracker.R

/**
 * Font families per the design system:
 *  Nunito — ExtraBold(800), Bold(700), SemiBold(600), Regular(400)
 *  Inter  — Medium(500), Regular(400), Light(300)
 */
val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

val InterFamily = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Font family used for amounts / numeric read-outs. */
val AmountFontFamily = InterFamily

/**
 * The named type scale from the design system.
 *  Display 48 / H1 28 / H2 22 / H3 18 / H4 16 / Body1 15 / Body2 13 /
 *  Caption 11 / Label 12
 */
object TypeScale {
    val Display = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp,
    )
    val H1 = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp,
    )
    val H2 = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
    )
    val H3 = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
    )
    val H4 = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
    )
    val Body1 = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp,
    )
    val Body2 = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
    )
    val Caption = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
    )
    val Label = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
    )
}

/** Material 3 roles mapped onto the design-system type scale. */
val MoneyMateTypography = Typography(
    displayLarge = TypeScale.Display,
    displayMedium = TypeScale.H1,
    displaySmall = TypeScale.H2,
    headlineLarge = TypeScale.H1,
    headlineMedium = TypeScale.H2,
    headlineSmall = TypeScale.H3,
    titleLarge = TypeScale.H2,
    titleMedium = TypeScale.H3,
    titleSmall = TypeScale.H4,
    bodyLarge = TypeScale.Body1,
    bodyMedium = TypeScale.Body2,
    bodySmall = TypeScale.Caption,
    labelLarge = TypeScale.Label.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = TypeScale.Label,
    labelSmall = TypeScale.Caption.copy(fontWeight = FontWeight.Medium),
)

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
