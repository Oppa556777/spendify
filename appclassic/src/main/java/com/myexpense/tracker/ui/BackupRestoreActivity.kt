package com.myexpense.tracker.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.util.Export
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs

class BackupRestoreActivity : Activity() {

    private var symbol: String = "$"
    private var pendingRestore: Uri? = null

    private lateinit var previewArea: LinearLayout
    private var previewText: String = ""

    private val REQ_CREATE_JSON = 1
    private val REQ_OPEN_JSON = 2
    private val REQ_CREATE_CSV = 3
    private val REQ_CREATE_PDF = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeChoice()
        super.onCreate(savedInstanceState)
        symbol = Prefs.currency(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            addView(Button(this@BackupRestoreActivity).apply {
                text = "‹"
                textSize = 20f
                setOnClickListener { finish() }
            })
            addView(TextView(this@BackupRestoreActivity).apply {
                text = "Backup & Export"
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setPadding(dp(12), 0, 0, 0)
                }
            })
        })

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(TextView(this).apply {
            text = "Everything is stored locally in files you choose. Nothing leaves your device — no internet, no cloud."
            setPadding(dp(16), dp(4), dp(16), dp(12))
            setTextColor(color(R.color.subtext))
        })

        body.addView(actionCard("💾", "Full backup (JSON)", "Categories, accounts, transactions, budgets") {
            createDocument("application/json", "moneymate-backup-${AppDb.todayIso()}.json", REQ_CREATE_JSON)
        })
        body.addView(actionCard("📥", "Restore backup", "Replace current data with a backup file") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQ_OPEN_JSON)
        })
        body.addView(actionCard("📊", "Export CSV", "All transactions, openable in Excel / Sheets") {
            createDocument("text/csv", "moneymate-transactions.csv", REQ_CREATE_CSV)
        })
        body.addView(actionCard("📄", "PDF report", "Monthly summary for ${Format.monthYear(AppDb.currentMonth())}") {
            createDocument("application/pdf", "moneymate-report-${AppDb.currentMonth()}.pdf", REQ_CREATE_PDF)
        })

        previewArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        body.addView(previewArea)

        root.addView(android.widget.ScrollView(this).apply {
            addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        setContentView(root)
    }

    private fun createDocument(mime: String, name: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, name)
        }
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_CREATE_JSON -> runCatching {
                val ok = Export.exportJson(this, uri)
                showMessage(if (ok) "Backup saved" else "Export failed")
            }.onFailure { showMessage("Export failed: ${it.message}") }

            REQ_OPEN_JSON -> runCatching {
                val obj = Export.parseJson(this, uri)
                previewText = Export.backupSummary(obj)
                pendingRestore = uri
                showPreview()
            }.onFailure { showMessage("Invalid backup: ${it.message}") }

            REQ_CREATE_CSV -> runCatching {
                val n = Export.exportCsv(this, uri, null, symbol)
                showMessage("CSV exported: $n transactions")
            }.onFailure { showMessage("CSV export failed: ${it.message}") }

            REQ_CREATE_PDF -> runCatching {
                val n = Export.exportPdf(this, uri, AppDb.currentMonth(), symbol)
                showMessage("PDF report saved ($n transactions)")
            }.onFailure { showMessage("PDF export failed: ${it.message}") }
        }
    }

    private fun actionCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpf(16f)
                setColor(color(R.color.card))
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(4), dp(16), dp(4))
            layoutParams = lp
            addView(TextView(this@BackupRestoreActivity).apply {
                text = emoji
                textSize = 22f
            })
            val col = LinearLayout(this@BackupRestoreActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(this@BackupRestoreActivity).apply {
                    text = subtitle.split("\n")[0]
                    textSize = 15f
                    setTextColor(color(R.color.text))
                })
                if (subtitle.contains("\n")) {
                    addView(TextView(this@BackupRestoreActivity).apply {
                        text = subtitle.split("\n")[1]
                        textSize = 12f
                        setTextColor(color(R.color.subtext))
                    })
                }
            }
            addView(col)
        }
        return card
    }

    private fun showMessage(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showPreview() {
        previewArea.removeAllViews()
        previewArea.addView(TextView(this).apply {
            text = previewText
            textSize = 13f
            setTextColor(color(R.color.text))
            setPadding(dp(16), dp(8), dp(16), dp(4))
        })
        previewArea.addView(Button(this).apply {
            text = "Restore this backup"
            setOnClickListener {
                val uri = pendingRestore
                if (uri != null) {
                    runCatching {
                        val obj = Export.parseJson(this@BackupRestoreActivity, uri)
                        Export.applyJson(this@BackupRestoreActivity, obj)
                        showMessage("Restore complete")
                        previewArea.removeAllViews()
                    }.onFailure { showMessage("Restore failed: ${it.message}") }
                }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(4), dp(16), dp(8))
            }
        })
    }
}
