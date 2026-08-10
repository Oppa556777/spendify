package com.myexpense.tracker.utils

/** Converts an opaque ARGB color Long (e.g. 0xFF4CAF50) to "#RRGGBB". */
fun Long.toHexColor(): String = String.format("#%06X", 0xFFFFFFL and this)

/** Parses "#RRGGBB" (or "#AARRGGBB") back into an opaque ARGB Long. */
fun String.toColorLong(): Long = try {
    val cleaned = removePrefix("#").removePrefix("0x")
    0xFF000000L or cleaned.toLong(16)
} catch (e: NumberFormatException) {
    0xFF4CAF50L
}
