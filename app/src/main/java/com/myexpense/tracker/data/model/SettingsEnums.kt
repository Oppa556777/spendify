package com.myexpense.tracker.data.model

/** Theme choices including AMOLED (pure black). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED;

    companion object {
        fun fromOrdinal(value: Int): ThemeMode = entries.getOrElse(value) { SYSTEM }
    }
}

/** Font size presets. */
enum class FontSize(val label: String, val scale: Float) {
    SMALL("Small", 0.9f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.15f);

    companion object {
        fun fromOrdinal(value: Int): FontSize = entries.getOrElse(value) { MEDIUM }
    }
}

/** Date display formats. */
enum class DateFormat(val label: String, val pattern: String) {
    DDMMYYYY("DD/MM/YYYY", "dd/MM/yyyy"),
    MMDDYYYY("MM/DD/YYYY", "MM/dd/yyyy"),
    YYYYMMDD("YYYY-MM-DD", "yyyy-MM-dd");

    companion object {
        fun fromOrdinal(value: Int): DateFormat = entries.getOrElse(value) { DDMMYYYY }
    }
}

/** First day of the week. */
enum class WeekStart(val label: String, val isoDay: Int) {
    MONDAY("Monday", 1),
    SUNDAY("Sunday", 7),
    SATURDAY("Saturday", 6);
}

/** Number grouping styles. */
enum class NumberFormat(val label: String) {
    INDIAN("1,00,000 (Indian)"),
    INTERNATIONAL("100,000 (International)");
}

/** How long after backgrounding before the lock re-engages. */
enum class LockDelay(val label: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0),
    ONE_MINUTE("1 min", 60_000L),
    FIVE_MINUTES("5 min", 300_000L),
    FIFTEEN_MINUTES("15 min", 900_000L);
}

/** Accent color presets for theming. */
enum class Accent(val label: String, val color: Long) {
    PURPLE("Purple", 0xFF6C63FF),
    BLUE("Blue", 0xFF3B82F6),
    GREEN("Green", 0xFF2E7D32),
    TEAL("Teal", 0xFF00897B),
    ORANGE("Orange", 0xFFF57C00),
    PINK("Pink", 0xFFEC407A),
    RED("Red", 0xFFE53935),
    INDIGO("Indigo", 0xFF3949AB),
    CYAN("Cyan", 0xFF00ACC1),
    YELLOW("Yellow", 0xFFFBC02D);

    companion object {
        fun fromName(name: String?): Accent = entries.firstOrNull { it.name == name } ?: PURPLE
    }
}
