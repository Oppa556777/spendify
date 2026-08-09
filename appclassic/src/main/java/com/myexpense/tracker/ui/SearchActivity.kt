package com.myexpense.tracker.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.TxType
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs

class SearchActivity : Activity() {

    private lateinit var db: AppDb
    private var symbol: String = "$"

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeChoice()
        super.onCreate(savedInstanceState)
        db = AppDb.get(this)
        symbol = Prefs.currency(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            addView(Button(this@SearchActivity).apply {
                text = "‹"
                textSize = 20f
                setOnClickListener { finish() }
            })
            addView(TextView(this@SearchActivity).apply {
                text = "Search"
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setPadding(dp(12), 0, 0, 0)
                }
            })
        })

        val input = EditText(this).apply {
            hint = "Search notes or categories…"
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(16), dp(4), dp(16), dp(4))
        })

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                render(body, s?.toString() ?: "")
            }
        })
        render(body, "")
    }

    private fun render(body: LinearLayout, query: String) {
        body.removeAllViews()
        if (query.isBlank()) {
            body.addView(TextView(this).apply {
                text = "Type to search your transactions."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
            return
        }
        val rows = db.getTransactions(null, null, null, null, query)
        if (rows.isEmpty()) {
            body.addView(TextView(this).apply {
                text = "No results for \"$query\"."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
            return
        }
        val catMap = db.getAllCategories().associateBy { it.id }
        rows.forEach { t ->
            val cat = t.categoryId?.let { catMap[it] }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(
                        android.content.Intent(
                            this@SearchActivity,
                            AddEditTransactionActivity::class.java
                        ).putExtra("id", t.id)
                    )
                }
                setOnLongClickListener {
                    confirm(this@SearchActivity, "Delete transaction", "Delete this entry?") {
                        db.deleteTransaction(t.id)
                        render(body, query)
                    }
                    true
                }
            }
            row.addView(CircleBadge(this@SearchActivity).apply {
                emoji = cat?.icon ?: "❓"
                badgeColor = cat?.color ?: color(R.color.subtext)
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
            val col = LinearLayout(this@SearchActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(this@SearchActivity).apply {
                    text = t.note.ifBlank { cat?.name ?: if (t.type == TxType.EXPENSE) "Expense" else "Income" }
                    textSize = 15f
                    setTextColor(color(R.color.text))
                })
                addView(TextView(this@SearchActivity).apply {
                    text = "${cat?.name ?: "Uncategorized"} • ${Format.fullDate(t.date)}"
                    textSize = 12f
                    setTextColor(color(R.color.subtext))
                })
            }
            row.addView(col)
            row.addView(TextView(this@SearchActivity).apply {
                text = (if (t.type == TxType.EXPENSE) "-" else "+") + Format.money(t.amount, symbol)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(if (t.type == TxType.EXPENSE) color(R.color.text) else color(R.color.income))
            })
            body.addView(row)
        }
    }
}
