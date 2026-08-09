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
