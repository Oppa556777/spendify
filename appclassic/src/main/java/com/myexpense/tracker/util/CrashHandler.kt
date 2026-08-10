package com.myexpense.tracker.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captures uncaught crashes and shows an in-app error report (ErrorActivity)
 * instead of letting the process die silently. The exception is swallowed so
 * the user always sees something actionable.
 */
object CrashHandler {

    private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()
                val file = File(appContext.filesDir, "crash.log")
                try {
                    file.appendText(
                        "\n===== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())} =====\n" +
                            "$stack\n"
                    )
                } catch (_: Throwable) {
                }

                val message = throwable.javaClass.name + (throwable.message?.let { ": $it" } ?: "")

                // Show the error report in-app; do NOT re-throw (keeps process alive).
                Handler(Looper.getMainLooper()).post {
                    try {
                        val intent = Intent(appContext, com.myexpense.tracker.ui.ErrorActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra("error", message)
                            putExtra("stack", stack)
                        }
                        appContext.startActivity(intent)
                    } catch (_: Throwable) {
                        android.util.Log.e("MoneyMate", "crash handler failed", throwable)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun deviceInfo(): String = buildString {
        append("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        append("Build: ${Build.DISPLAY}\n")
    }

    fun lastCrash(context: Context): String? =
        File(context.filesDir, "crash.log").takeIf { it.exists() }?.readText()?.takeLast(6000)
}
