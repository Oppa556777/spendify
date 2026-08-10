package com.myexpense.tracker.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.myexpense.tracker.R
import com.myexpense.tracker.util.CrashHandler

/**
 * Full-screen error report shown when the app would otherwise crash.
 * Includes device info, the stack trace, a copy button and a "reset data"
 * button (deletes the local database + settings) — the usual fix when an
 * older install left a corrupt database behind.
 */
class ErrorActivity : Activity() {

    private var errorText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            applyThemeChoice()
        } catch (_: Throwable) {
        }
        super.onCreate(savedInstanceState)

        errorText = intent.getStringExtra("error") ?: "Unknown error"
        val stack = intent.getStringExtra("stack") ?: CrashHandler.lastCrash(this) ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }

        root.addView(TextView(this).apply {
            text = "MoneyMate hit an error"
            textSize = 22f
            typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color(R.color.text))
            setPadding(dp(24), dp(24), dp(24), dp(8))
        })

        root.addView(TextView(this).apply {
            text = CrashHandler.deviceInfo() + "\n"
            textSize = 12f
            setTextColor(color(R.color.subtext))
            setPadding(dp(24), 0, dp(24), 0)
        })

        val body = TextView(this).apply {
            text = "$errorText\n\n$stack"
            textSize = 11f
            setTextColor(color(R.color.text))
            setPadding(dp(24), dp(12), dp(24), dp(12))
        }
        root.addView(ScrollView(this).apply {
            addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Copy details
        root.addView(Button(this).apply {
            text = "Copy error details"
            setOnClickListener {
                val clip = ClipData.newPlainText("moneymate-crash", errorText + "\n\n" + stack + "\n\n" + CrashHandler.deviceInfo())
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(this@ErrorActivity, "Copied. Send this text to the developer.", Toast.LENGTH_LONG).show()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), 0)
            }
        })

        // Try again
        root.addView(Button(this).apply {
            text = "Try again"
            setOnClickListener {
                startActivity(Intent(this@ErrorActivity, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                finish()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), 0)
            }
        })

        // Reset app data (deletes the DB + settings, then restarts fresh)
        root.addView(Button(this).apply {
            text = "Reset app data (fixes corrupt data)"
            setOnClickListener {
                try {
                    deleteDatabase("moneymate.db")
                    getSharedPreferences("moneymate_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().apply()
                    fileList().filter { it.endsWith(".db") || it.endsWith(".db-journal") || it.endsWith(".db-wal") || it.endsWith(".db-shm") }
                        .forEach { deleteFile(it) }
                } catch (_: Throwable) {
                }
                Toast.makeText(this@ErrorActivity, "Data reset. Restarting…", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@ErrorActivity, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                finish()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(24), dp(8), dp(24), dp(24))
            }
        })

        setContentView(root)
    }
}
