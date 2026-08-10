package com.myexpense.tracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii per the design system:
 *  Cards 16 · Buttons 12 · Sheets 24 (top) · Chips 8 · FAB 16 · Dialog 20 · Inputs 12
 */
object Radii {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Fab = RoundedCornerShape(16.dp)
    val Dialog = RoundedCornerShape(20.dp)
    val Input = RoundedCornerShape(12.dp)
}

/** Material 3 shape slots aligned to the design system. */
val MoneyMateShapes = Shapes(
    extraSmall = Radii.Chip,
    small = Radii.Input,
    medium = Radii.Card,
    large = Radii.Dialog,
    extraLarge = Radii.Sheet,
)
