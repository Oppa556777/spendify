# MoneyMate – Expense Tracker ProGuard rules

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Gson (backup/restore models)
-keep class com.myexpense.tracker.data.model.backup.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Vico charts
-dontwarn com.patrykandpatrick.vico.**

# OpenCSV
-dontwarn org.apache.commons.**
-dontwarn org.apache.logging.log4j.**

# Kotlinx coroutines
-dontwarn kotlinx.coroutines.**
