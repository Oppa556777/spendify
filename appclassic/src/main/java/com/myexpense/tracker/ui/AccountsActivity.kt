package com.myexpense.tracker.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AccountType
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs
import com.myexpense.tracker.util.Seed

class AccountsActivity : Activity() {

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
        root.addView(toolbar())

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        refresh(body)
        root.addView(body)

        root.addView(Button(this).apply {
            text = "+ Add Account"
            setOnClickListener { showAccountDialog(null) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(8), dp(16), dp(16))
            }
        })

        setContentView(root)
    }

    private fun toolbar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            addView(Button(this@AccountsActivity).apply {
                text = "‹"
                textSize = 20f
                setOnClickListener { finish() }
            })
            addView(TextView(this@AccountsActivity).apply {
                text = "Accounts"
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setPadding(dp(12), 0, 0, 0)
                }
            })
        }

    private fun refresh(body: LinearLayout) {
        body.removeAllViews()
        val accounts = db.getAccounts()
        val balances = db.getBalances()

        val total = accounts.sumOf { it.initialBalance + (balances[it.id] ?: 0L) }
        body.addView(TextView(this).apply {
            text = "Total balance: ${Format.money(total, symbol)}"
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(color(R.color.text))
            setPadding(dp(16), dp(12), dp(16), dp(8))
        })

        if (accounts.isEmpty()) {
            body.addView(TextView(this).apply {
                text = "No accounts yet.\nAdd your cash, bank or card accounts."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
            return
        }

        accounts.forEach { account ->
            val balance = account.initialBalance + (balances[account.id] ?: 0L)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener { showAccountDialog(account.id) }
                setOnLongClickListener {
                    confirm(this@AccountsActivity, "Delete account", "Delete \"${account.name}\"? Transactions keep their entries.") {
                        db.deleteAccount(account.id)
                        refresh(body)
                    }
                    true
                }
            }
            row.addView(CircleBadge(this@AccountsActivity).apply {
                emoji = account.icon
                badgeColor = account.color
            }, LinearLayout.LayoutParams(dp(42), dp(42)))
            val col = LinearLayout(this@AccountsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(this@AccountsActivity).apply {
                    text = account.name
                    textSize = 15f
                    setTextColor(color(R.color.text))
                })
                addView(TextView(this@AccountsActivity).apply {
                    text = account.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    textSize = 12f
                    setTextColor(color(R.color.subtext))
                })
            }
            row.addView(col)
            row.addView(TextView(this@AccountsActivity).apply {
                text = Format.money(balance, symbol)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
            })
            body.addView(row)
        }
    }

    private fun showAccountDialog(accountId: Long?) {
        val existing = accountId?.let { db.getAccount(it) }
        var name = existing?.name ?: ""
        var initial = existing?.let { Format.money(it.initialBalance, "") } ?: ""
        var type = existing?.type ?: AccountType.CASH
        var color = existing?.color ?: Seed.colorPalette.first()
        var icon = existing?.icon ?: "💳"

        val builder = AlertDialogHelper.form(this, if (existing == null) "New Account" else "Edit Account")
        builder.addLabel("Name")
        builder.addTextField(name, "e.g. Cash, Bank, Card") { name = it }
        builder.addLabel("Opening balance")
        builder.addAmountField(initial) { initial = it }

        builder.addLabel("Type")
        val typeNames = AccountType.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }.toTypedArray()
        builder.addSpinner(typeNames, AccountType.entries.indexOf(type)) { idx ->
            type = AccountType.entries[idx]
        }

        builder.addLabel("Icon")
        val iconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val pickableIcons = listOf("💵", "🏦", "💳", "📱", "📈", "💰", "👛", "🏧")
        pickableIcons.forEach { emoji ->
            iconRow.addView(TextView(this).apply {
                text = emoji
                textSize = 20f
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    icon = emoji
                    AlertDialogHelper.toast(this@AccountsActivity, "Icon: $emoji")
                }
                background = GradientDrawable().apply {
                    cornerRadius = dpf(10f)
                    setColor(color(R.color.card))
                }
                setPadding(dp(8), dp(4), dp(8), dp(4))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                }
            })
        }
        builder.addView(iconRow)

        builder.addLabel("Colour")
        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        Seed.colorPalette.forEach { c ->
            colorRow.addView(TextView(this).apply {
                text = if (c == color) "●" else "○"
                textSize = 22f
                setTextColor(c)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    color = c
                    AlertDialogHelper.toast(this@AccountsActivity, "Colour set")
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(4)
                }
            })
        }
        builder.addView(colorRow)

        builder.addSave("Save") { dialog ->
            if (name.isBlank()) {
                AlertDialogHelper.toast(this, "Enter a name")
                return@addSave
            }
            val amount = initial.toDoubleOrNull() ?: 0.0
            db.insertAccount(
                com.myexpense.tracker.db.Account(
                    id = existing?.id ?: 0,
                    name = name.trim(),
                    type = type,
                    initialBalance = (amount * 100).toLong(),
                    color = color,
                    icon = icon,
                )
            )
            dialog.dismiss()
            recreate()
        }
        builder.show()
    }
}
