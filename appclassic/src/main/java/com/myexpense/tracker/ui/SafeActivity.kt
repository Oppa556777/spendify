package com.myexpense.tracker.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.myexpense.tracker.util.CrashHandler
import java.io.File

/**
 * Bulletproof launcher screen. Uses ONLY the default system theme and plain
 * framework widgets, so it cannot fail on resources/theme. It shows the
 * device info, the last saved crash log, and lets the user launch the app.
 * If the real app still crashes, this screen stays reachable and shows why.
 */
class SafeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Intentionally NO custom theme — default theme only.
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(TextView(this).apply {
            text = "💰 MoneyMate"
            textSize = 26f
            typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(24), dp(32), dp(24), dp(4))
        })

        root.addView(TextView(this).apply {
            text = "v1.0.4 — safe launcher"
            textSize = 13f
            setPadding(dp(24), 0, dp(24), dp(12))
        })

        val info = StringBuilder()
        info.append("Android ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        info.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
        info.append("minSdk required: 26 (Android 8.0)\n")

        val infoText = TextView(this).apply {
            text = info.toString()
            textSize = 13f
            setPadding(dp(24), dp(8), dp(24), dp(4))
        }
        root.addView(infoText)

        // Last crash log (if any)
        val crashLog = CrashHandler.lastCrash(this) ?: "(no crash log yet)"
        val crashView = TextView(this).apply {
            text = "Last crash:\n$crashLog"
            textSize = 11f
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        root.addView(
            ScrollView(this).apply {
                addView(crashView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        // Copy diagnostics
        root.addView(Button(this).apply {
            text = "Copy diagnostics"
            setOnClickListener {
                val text = info.toString() + "\n" + crashLog
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("moneymate-diag", text))
                Toast.makeText(this@SafeActivity, "Copied — send this text", Toast.LENGTH_LONG).show()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), 0)
            }
        })

        // Launch the app (all real initialization happens here, in try/catch)
        root.addView(Button(this).apply {
            text = "Open MoneyMate"
            setOnClickListener {
                try {
                    startActivity(Intent(this@SafeActivity, LockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                } catch (t: Throwable) {
                    crashView.text = "Error opening app:\n" + t.javaClass.name + ": " + (t.message ?: "") +
                        "\n\n" + crashLog
                }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), 0)
            }
        })

        // Reset data
        root.addView(Button(this).apply {
            text = "Reset app data"
            setOnClickListener {
                try {
                    deleteDatabase("moneymate.db")
                    getSharedPreferences("moneymate_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    fileList().filter {
                        it.endsWith(".db") || it.endsWith(".db-journal") || it.endsWith(".db-wal") || it.endsWith(".db-shm")
                    }.forEach { deleteFile(it) }
                    File(filesDir, "crash.log").delete()
                    Toast.makeText(this@SafeActivity, "Data reset", Toast.LENGTH_SHORT).show()
                    crashView.text = "(no crash log yet)"
                } catch (t: Throwable) {
                    Toast.makeText(this@SafeActivity, "Reset failed: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), dp(24))
            }
        })

        setContentView(root)
    }

    private fun dp(v: Int): Int = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()
}
