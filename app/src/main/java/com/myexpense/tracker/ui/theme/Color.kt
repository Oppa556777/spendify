package com.myexpense.tracker.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// LIGHT THEME
// ─────────────────────────────────────────────────────────────────────────────
val LightPrimary = Color(0xFF6C63FF)          // Purple
val LightPrimaryVariant = Color(0xFF5A52E0)
val LightSecondary = Color(0xFF03DAC6)        // Teal
val LightBackground = Color(0xFFF5F5F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0EFFE)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1A1A2E)
val LightOnSurface = Color(0xFF1A1A2E)
val LightOnSurfaceVariant = Color(0xFF6B6B8A)
val LightOutline = Color(0xFFC4C4D4)
val LightOutlineVariant = Color(0xFFE4E4F0)
val LightError = Color(0xFFE53935)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightSurfaceContainer = Color(0xFFF1F0FA)
val LightSurfaceContainerHigh = Color(0xFFEBEAF6)
val LightSurfaceContainerHighest = Color(0xFFE5E4F2)

// ─────────────────────────────────────────────────────────────────────────────
// DARK THEME
// ─────────────────────────────────────────────────────────────────────────────
val DarkPrimary = Color(0xFF7C74FF)           // Lighter Purple
val DarkPrimaryVariant = Color(0xFF6C63FF)
val DarkSecondary = Color(0xFF03DAC6)
val DarkBackground = Color(0xFF0D0D1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkSurfaceVariant = Color(0xFF252540)
val DarkOnPrimary = Color(0xFF000000)
val DarkOnBackground = Color(0xFFE8E8FF)
val DarkOnSurface = Color(0xFFE8E8FF)
val DarkOnSurfaceVariant = Color(0xFFA8A8C8)
val DarkOutline = Color(0xFF404060)
val DarkOutlineVariant = Color(0xFF33334F)
val DarkError = Color(0xFFCF6679)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkSurfaceContainer = Color(0xFF22223A)
val DarkSurfaceContainerHigh = Color(0xFF2C2C48)
val DarkSurfaceContainerHighest = Color(0xFF363658)

// ─────────────────────────────────────────────────────────────────────────────
// SEMANTIC
// ─────────────────────────────────────────────────────────────────────────────
val IncomeGreen = Color(0xFF00C853)
val ExpenseRed = Color(0xFFFF1744)
val TransferBlue = Color(0xFF2979FF)
val WarningOrange = Color(0xFFFF9800)
val SuccessGreen = Color(0xFF43A047)

// ─────────────────────────────────────────────────────────────────────────────
// CATEGORY COLOR PALETTE (assigned in order)
// ─────────────────────────────────────────────────────────────────────────────
val CategoryPalette = listOf(
    Color(0xFFFF6B6B),  // Red-Pink
    Color(0xFFFFA94D),  // Orange
    Color(0xFFFFD43B),  // Yellow
    Color(0xFF69DB7C),  // Green
    Color(0xFF4DABF7),  // Blue
    Color(0xFFDA77F2),  // Purple
    Color(0xFFF783AC),  // Pink
    Color(0xFF63E6BE),  // Teal
    Color(0xFF74C0FC),  // Light Blue
    Color(0xFFA9E34B),  // Lime
    Color(0xFFFFD8A8),  // Peach
    Color(0xFFE599F7),  // Lavender
)

/** Category colors as opaque ARGB Longs (for the database). */
val CategoryColorLongs: List<Long> = CategoryPalette.map { it.value.toLong() or 0xFF000000 }

// ─────────────────────────────────────────────────────────────────────────────
// ACCOUNT CARD GRADIENTS
// ─────────────────────────────────────────────────────────────────────────────
data class AccountGradient(val start: Long, val end: Long)

object AccountGradients {
    val BLUE_BANK = AccountGradient(0xFF667EEA, 0xFF764BA2)
    val GREEN_CASH = AccountGradient(0xFF11998E, 0xFF38EF7D)
    val PURPLE_CREDIT = AccountGradient(0xFFDA22FF, 0xFF9733EE)
    val ORANGE_WALLET = AccountGradient(0xFFF7971E, 0xFFFFD200)
    val TEAL_SAVINGS = AccountGradient(0xFF0F2027, 0xFF2C5364)
    val PINK_INVEST = AccountGradient(0xFFEE0979, 0xFFFF6A00)

    /** Stable per-account gradient selection. */
    fun forAccount(accountId: Long): AccountGradient = when ((accountId % 6).toInt()) {
        0 -> BLUE_BANK
        1 -> GREEN_CASH
        2 -> PURPLE_CREDIT
        3 -> ORANGE_WALLET
        4 -> TEAL_SAVINGS
        else -> PINK_INVEST
    }
}
