package com.myexpense.tracker.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.TxType
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs

class AddEditTransactionActivity : Activity() {

    private lateinit var db: AppDb
    private var symbol: String = "$"
    private var editingId = 0L
    private var type = TxType.EXPENSE
    private var categoryId: Long? = null
    private var accountId: Long? = null
    private var dateIso = AppDb.todayIso()

    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var categoryLabel: TextView
    private lateinit var dateLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeChoice()
        super.onCreate(savedInstanceState)
        db = AppDb.get(this)
        symbol = Prefs.currency(this)

        editingId = intent.getLongExtra("id", 0L)
        val presetType = intent.getStringExtra("type")
        if (presetType != null) type = TxType.valueOf(presetType)

        if (editingId != 0L) {
            db.getTransaction(editingId)?.let { t ->
                type = t.type
                categoryId = t.categoryId
                accountId = t.accountId
                dateIso = t.date
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }
        root.addView(toolbar())

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }

        // Type toggle
        val typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val expenseBtn = toggleButton("Expense") { setType(TxType.EXPENSE) }
        val incomeBtn = toggleButton("Income") { setType(TxType.INCOME) }
        typeRow.addView(expenseBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        typeRow.addView(incomeBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(typeRow)
        typeButtons = Pair(expenseBtn, incomeBtn)
        refreshTypeButtons()

        // Amount
        body.addView(TextView(this).apply {
            text = "Amount"
            textSize = 12f
            setTextColor(color(R.color.primary))
            setPadding(0, dp(14), 0, dp(4))
        })
        amountInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            hint = "0.00"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dpf(12f)
                setColor(color(R.color.card))
            }
        }
        body.addView(amountInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Category picker
        body.addView(TextView(this).apply {
            text = "Category"
            textSize = 12f
            setTextColor(color(R.color.primary))
            setPadding(0, dp(14), 0, dp(4))
        })
        categoryLabel = TextView(this).apply {
            text = "Tap to choose"
            textSize = 15f
            setTextColor(color(R.color.text))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dpf(12f)
                setColor(color(R.color.card))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showCategoryPicker() }
        }
        body.addView(categoryLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Account
        body.addView(TextView(this).apply {
            text = "Account"
            textSize = 12f
            setTextColor(color(R.color.primary))
            setPadding(0, dp(14), 0, dp(4))
        })
        val accounts = db.getAccounts()
        val accountSpinner = Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@AddEditTransactionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                accounts.map { it.name }
            )
            if (accounts.isNotEmpty()) {
                val idx = accounts.indexOfFirst { it.id == accountId }.coerceAtLeast(0)
                setSelection(idx)
                accountId = accounts[idx].id
            }
        }
        accountSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in accounts.indices) accountId = accounts[position].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        if (accounts.isNotEmpty()) body.addView(accountSpinner)
        else body.addView(TextView(this).apply {
            text = "No accounts yet — add one in More → Accounts"
            setTextColor(color(R.color.subtext))
        })

        // Date
        body.addView(TextView(this).apply {
            text = "Date"
            textSize = 12f
            setTextColor(color(R.color.primary))
            setPadding(0, dp(14), 0, dp(4))
        })
        dateLabel = TextView(this).apply {
            text = Format.fullDate(dateIso)
            textSize = 15f
            setTextColor(color(R.color.text))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dpf(12f)
                setColor(color(R.color.card))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showDatePicker() }
        }
        body.addView(dateLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Note
        body.addView(TextView(this).apply {
            text = "Note (optional)"
            textSize = 12f
            setTextColor(color(R.color.primary))
            setPadding(0, dp(14), 0, dp(4))
        })
        noteInput = EditText(this).apply {
            isSingleLine = true
            hint = "e.g. Groceries at the market"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dpf(12f)
                setColor(color(R.color.card))
            }
        }
        body.addView(noteInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Prefill when editing
        if (editingId != 0L) {
            db.getTransaction(editingId)?.let { t ->
                amountInput.setText(Format.money(t.amount, ""))
                noteInput.setText(t.note)
            }
        }

        // Save
        body.addView(Button(this).apply {
            text = if (editingId == 0L) "Save Transaction" else "Update Transaction"
            setOnClickListener { save() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20)
            }
        })

        root.addView(body)
        setContentView(root)
        refreshCategoryLabel()
        refreshDateLabel()
    }

    private var typeButtons: Pair<Button, Button>? = null

    private fun toolbar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            addView(Button(this@AddEditTransactionActivity).apply {
                text = "‹"
                textSize = 20f
                setOnClickListener { finish() }
            })
            addView(TextView(this@AddEditTransactionActivity).apply {
                text = if (editingId == 0L) "Add Transaction" else "Edit Transaction"
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setPadding(dp(12), 0, 0, 0)
                }
            })
        }

    private fun toggleButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun setType(newType: TxType) {
        type = newType
        refreshTypeButtons()
    }

    private fun refreshTypeButtons() {
        val (expenseBtn, incomeBtn) = typeButtons ?: return
        expenseBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (type == TxType.EXPENSE) color(R.color.expense) else color(R.color.card)
        )
        expenseBtn.setTextColor(if (type == TxType.EXPENSE) Color.WHITE else color(R.color.text))
        incomeBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (type == TxType.INCOME) color(R.color.income) else color(R.color.card)
        )
        incomeBtn.setTextColor(if (type == TxType.INCOME) Color.WHITE else color(R.color.text))
    }

    private fun showCategoryPicker() {
        val cats = db.getCategories(type)
        val names = cats.map { it.name }.toTypedArray()
        val icons = cats.map { it.icon }.toTypedArray()
        AlertDialogHelper.pick(this, "Choose category", names, icons) { index ->
            categoryId = cats[index].id
            refreshCategoryLabel()
        }
    }

    private fun refreshCategoryLabel() {
        val cat = categoryId?.let { db.getCategory(it) }
        categoryLabel.text = if (cat != null) "${cat.icon}  ${cat.name}" else "Tap to choose"
    }

    private fun showDatePicker() {
        val parts = dateIso.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt() - 1
        val d = parts[2].toInt()
        DatePickerDialog(this, { _, year, month, day ->
            dateIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            refreshDateLabel()
        }, y, m, d).show()
    }

    private fun refreshDateLabel() {
        dateLabel.text = Format.fullDate(dateIso)
    }

    private fun save() {
        val value = amountInput.text.toString().toDoubleOrNull() ?: 0.0
        if (value <= 0) {
            AlertDialogHelper.toast(this, "Enter an amount greater than zero")
            return
        }
        val amountMinor = (value * 100).toLong().coerceAtLeast(1)
        val t = com.myexpense.tracker.db.Transaction(
            id = editingId,
            type = type,
            amount = amountMinor,
            categoryId = categoryId,
            accountId = accountId,
            note = noteInput.text.toString().trim(),
            date = dateIso
        )
        if (editingId == 0L) db.insertTransaction(t) else db.updateTransaction(t)
        setResult(RESULT_OK)
        finish()
    }
}
