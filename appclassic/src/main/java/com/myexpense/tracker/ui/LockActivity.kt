package com.myexpense.tracker.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.util.Prefs

/**
 * Launcher activity. When the biometric/device-credential lock is enabled the
 * user must confirm their device credential (fingerprint / face / PIN /
 * pattern) before entering the app. Uses only framework APIs.
 */
class LockActivity : Activity() {

    private val requestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeChoice()
        super.onCreate(savedInstanceState)

        val enabled = Prefs.biometricEnabled(this)
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!enabled || !keyguard.isDeviceSecure) {
            goToMain()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(color(R.color.bg))
        }
        root.addView(TextView(this).apply {
            text = "🔒"
            textSize = 56f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "MoneyMate is locked"
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(color(R.color.text))
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(8), dp(24), dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Use your fingerprint, face or device credential to continue."
            textSize = 14f
            setTextColor(color(R.color.subtext))
            gravity = Gravity.CENTER
            setPadding(dp(32), 0, dp(32), dp(24))
        })
        root.addView(Button(this).apply {
            text = "Unlock"
            setOnClickListener { requestCredential() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        setContentView(root)
    }

    private fun requestCredential() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val intent = keyguard.createConfirmDeviceCredentialIntent("MoneyMate", "Unlock your expense tracker")
        if (intent != null) {
            startActivityForResult(intent, requestCode)
        } else {
            goToMain()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == RESULT_OK) {
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
