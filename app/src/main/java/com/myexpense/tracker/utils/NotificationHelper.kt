package com.myexpense.tracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myexpense.tracker.R
import javax.inject.Inject
import javax.inject.Singleton

/** Sends in-app notifications (achievements, recurring reminders). */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
) {

    companion object {
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val CHANNEL_RECURRING = "recurring"
    }

    init {
        createChannel(CHANNEL_ACHIEVEMENTS, "Achievements", NotificationManager.IMPORTANCE_DEFAULT)
        createChannel(CHANNEL_RECURRING, "Recurring transactions", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun createChannel(id: String, name: String, importance: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(id, name, importance).apply {
                description = "MoneyMate $name notifications"
            }
        )
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(id: Int, channel: String, title: String, text: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_splash_wallet)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    fun showAchievementUnlocked(title: String) {
        show(title.hashCode(), CHANNEL_ACHIEVEMENTS, "🏆 Achievement unlocked!", title)
    }

    fun showRecurringDue(count: Int) {
        show(999, CHANNEL_RECURRING, "Recurring transactions due", "$count recurring transactions are due today")
    }
}
