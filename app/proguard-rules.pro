# MoneyMate – Expense Tracker ProGuard/R8 rules

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# ── Vico charts ──────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ── Kotlinx coroutines ───────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

# ── Coil (image loading) ─────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Gson (backup/restore models) ─────────────────────────────────────────────
-keep class com.myexpense.tracker.data.model.backup.** { *; }
-keep class com.myexpense.tracker.data.model.** { *; }
-keep class com.myexpense.tracker.data.database.entity.** { *; }

# ── OpenCSV ──────────────────────────────────────────────────────────────────
-dontwarn org.apache.commons.**
-dontwarn org.apache.logging.log4j.**

# ── Misc ─────────────────────────────────────────────────────────────────────
-keepattributes InnerClasses
