package com.myexpense.tracker.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captures uncaught crashes: writes the full stack trace to
 * filesDir/crash.log and shows a Toast with the exception class + message so
 * the user can report it. The app then finishes instead of hanging.
 */
object CrashHandler {

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()
                val file = File(appContext.filesDir, "crash.log")
                file.appendText(
                    "\n===== ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())} =====\n" +
                        "$stack\n"
                )
                val message = throwable.javaClass.name +
                    (throwable.message?.let { ": $it" } ?: "")
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(appContext, "MoneyMate error: $message", Toast.LENGTH_LONG).show()
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Reads the last crash log (for debugging via adb / file manager). */
    fun lastCrash(context: Context): String? =
        File(context.filesDir, "crash.log").takeIf { it.exists() }?.readText()?.takeLast(4000)
}
