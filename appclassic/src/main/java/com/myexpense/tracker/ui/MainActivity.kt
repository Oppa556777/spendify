package com.myexpense.tracker.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.Category
import com.myexpense.tracker.db.TxType
import com.myexpense.tracker.ui.widgets.BarChartView
import com.myexpense.tracker.ui.widgets.DonutChartView
import com.myexpense.tracker.util.Format
import com.myexpense.tracker.util.Prefs
import com.myexpense.tracker.util.Seed

class MainActivity : Activity() {

    private lateinit var db: AppDb
    private lateinit var contentFrame: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private var currentTab = 0
    private var symbol: String = "$"
    private var month = AppDb.currentMonth()
    private var txTypeFilter: TxType? = null
    private var txCategoryFilter: Long? = null
    private var txAccountFilter: Long? = null
    private var budgetMonth = AppDb.currentMonth()
    private var statsMonth = AppDb.currentMonth()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            applyThemeChoice()
            super.onCreate(savedInstanceState)
            db = AppDb.get(this)
            Seed.seedIfNeeded(this)
            symbol = Prefs.currency(this)
            month = AppDb.currentMonth()

            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(color(R.color.bg))
            }
            contentFrame = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            bottomBar = buildBottomBar()
            root.addView(contentFrame)
            root.addView(bottomBar)
            setContentView(root)

            showTab(0)
        } catch (t: Throwable) {
            // Never die silently: show the in-app error report (with Reset data).
            try {
                android.util.Log.e("MoneyMate", "startup crash", t)
                val sw = java.io.StringWriter()
                t.printStackTrace(java.io.PrintWriter(sw))
                startActivity(
                    Intent(this, ErrorActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra("error", t.javaClass.name + ": " + (t.message ?: ""))
                        putExtra("stack", sw.toString())
                    }
                )
                finish()
            } catch (_: Throwable) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        symbol = Prefs.currency(this)
        showTab(currentTab)
    }

    // ── Tab shell ───────────────────────────────────────────────────────────
    private fun buildBottomBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(color(R.color.card))
            elevation = dpf(8f)
        }
        val tabs = listOf(
            Triple("🏠", "Home", 0),
            Triple("💸", "Spends", 1),
            Triple("📊", "Stats", 2),
            Triple("🎯", "Budgets", 3),
            Triple("⚙️", "More", 4)
        )
        val tabViews = tabs.map { (emoji, label, index) ->
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(6), dp(4), dp(6))
                isClickable = true
                isFocusable = true
                setOnClickListener { showTab(index) }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@MainActivity).apply {
                    text = emoji
                    textSize = 18f
                })
                addView(TextView(this@MainActivity).apply {
                    text = label
                    textSize = 10f
                    tag = "tablabel_$index"
                    setTextColor(color(R.color.subtext))
                })
            }
        }
        tabViews.forEach { bar.addView(it) }
        return bar
    }

    private fun setTabLabel(index: Int, selected: Boolean) {
        val label = bottomBar.findViewWithTag<TextView>("tablabel_$index") ?: return
        label.setTextColor(
            if (selected) color(R.color.primary) else color(R.color.subtext)
        )
        label.typeface = Typeface.create(
            label.typeface,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun showTab(index: Int) {
        currentTab = index
        setTabLabel(0, index == 0)
        setTabLabel(1, index == 1)
        setTabLabel(2, index == 2)
        setTabLabel(3, index == 3)
        setTabLabel(4, index == 4)
        contentFrame.removeAllViews()
        when (index) {
            0 -> contentFrame.addView(buildHomeTab())
            1 -> contentFrame.addView(buildTransactionsTab())
            2 -> contentFrame.addView(buildStatsTab())
            3 -> contentFrame.addView(buildBudgetsTab())
            4 -> contentFrame.addView(buildSettingsTab())
        }
    }

    private fun scroll(inner: View): ScrollView = ScrollView(this).apply {
        addView(inner, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    // ── Home tab ────────────────────────────────────────────────────────────
    private fun buildHomeTab(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val balance = db.getTotalBalance()
        val income = db.sumIncome(month)
        val expense = db.sumExpense(month)

        // Hero card
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dpf(22f)
                colors = intArrayOf(0xFF2E7D32.toInt(), 0xFF4CAF50.toInt())
                orientation = GradientDrawable.Orientation.TL_BR
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(16), dp(16), dp(4))
            layoutParams = lp
            addView(TextView(this@MainActivity).apply {
                text = "TOTAL BALANCE"
                textSize = 11f
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@MainActivity).apply {
                text = Format.money(balance, symbol)
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Across all accounts"
                textSize = 12f
                setTextColor(0xE6FFFFFF.toInt())
            })
        }
        col.addView(hero)

        // Income / expense chips
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(12), dp(16), 0)
            layoutParams = lp
            addView(statChip("Income", Format.money(income, symbol), color(R.color.income)))
            addView(statChip("Expenses", Format.money(expense, symbol), color(R.color.expense)))
        }
        col.addView(chips)

        // Quick add buttons
        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(12), dp(16), 0)
            layoutParams = lp
            addView(actionButton("−  Expense", color(R.color.expense)) {
                startActivity(Intent(this@MainActivity, AddEditTransactionActivity::class.java).putExtra("type", TxType.EXPENSE.name))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            })
            addView(actionButton("+  Income", color(R.color.income)) {
                startActivity(Intent(this@MainActivity, AddEditTransactionActivity::class.java).putExtra("type", TxType.INCOME.name))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            })
        }
        col.addView(quick)

        // Shortcuts
        col.addView(sectionLabel(this, "Manage"))
        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), 0)
            layoutParams = lp
            addView(shortcutChip("💳\nAccounts") { startActivity(Intent(this@MainActivity, AccountsActivity::class.java)) })
            addView(shortcutChip("🏷️\nCategories") { startActivity(Intent(this@MainActivity, CategoriesActivity::class.java)) })
            addView(shortcutChip("🔎\nSearch") { startActivity(Intent(this@MainActivity, SearchActivity::class.java)) })
            addView(shortcutChip("💾\nBackup") { startActivity(Intent(this@MainActivity, BackupRestoreActivity::class.java)) })
        }
        col.addView(shortcuts)

        // Recent transactions
        col.addView(sectionLabel(this, "Recent transactions"))
        val recent = db.getRecent(6)
        if (recent.isEmpty()) {
            col.addView(TextView(this).apply {
                text = "No transactions yet — tap Expense or Income to add your first entry."
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setTextColor(color(R.color.subtext))
            })
        } else {
            val catMap = db.getAllCategories().associateBy { it.id }
            val accMap = db.getAccounts().associateBy { it.id }
            recent.forEach { t ->
                val cat = t.categoryId?.let { catMap[it] }
                col.addView(txRow(t, cat?.name ?: "Uncategorized", cat?.icon ?: "❓", cat?.color ?: color(R.color.subtext), accMap[t.accountId]?.name))
            }
        }

        // Budget preview
        val budgets = db.getBudgetRows(month)
        if (budgets.isNotEmpty()) {
            col.addView(sectionLabel(this, "Budgets"))
            budgets.take(3).forEach { b ->
                col.addView(budgetRow(b.categoryName, b.categoryIcon, b.categoryColor, b.spent, b.budget.amount))
            }
        }

        col.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(24)) })
        return scroll(col)
    }

    private fun statChip(label: String, value: String, valueColor: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = label.toUpperCase()
                textSize = 11f
                setTextColor(color(R.color.subtext))
            })
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(valueColor)
            })
        }

    private fun actionButton(label: String, tint: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
            backgroundTintList = android.content.res.ColorStateList.valueOf(tint)
            setTextColor(Color.WHITE)
        }

    private fun shortcutChip(label: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(10), dp(4), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            background = GradientDrawable().apply {
                cornerRadius = dpf(14f)
                setColor(color(R.color.card))
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            addView(TextView(this@MainActivity).apply { text = label; textSize = 12f; gravity = Gravity.CENTER })
        }

    private fun txRow(
        t: com.myexpense.tracker.db.Transaction,
        categoryName: String,
        icon: String,
        iconColor: Int,
        accountName: String?
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(
                    Intent(this@MainActivity, AddEditTransactionActivity::class.java)
                        .putExtra("id", t.id)
                )
            }
            setOnLongClickListener {
                confirm(this@MainActivity, "Delete transaction", "Delete this ${if (t.type == TxType.EXPENSE) "expense" else "income"}?") {
                    db.deleteTransaction(t.id)
                    showTab(currentTab)
                }
                true
            }
        }
        row.addView(CircleBadge(this).apply {
            emoji = icon
            badgeColor = iconColor
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(12), 0, dp(8), 0)
            addView(TextView(this@MainActivity).apply {
                text = t.note.ifBlank { categoryName }
                textSize = 15f
                setTextColor(color(R.color.text))
            })
            addView(TextView(this@MainActivity).apply {
                text = listOfNotNull(categoryName, accountName).joinToString(" • ") + " • " + Format.shortDate(t.date)
                textSize = 12f
                setTextColor(color(R.color.subtext))
            })
        }
        row.addView(col)
        row.addView(TextView(this).apply {
            text = (if (t.type == TxType.EXPENSE) "-" else "+") + Format.money(t.amount, symbol)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(if (t.type == TxType.EXPENSE) color(R.color.text) else color(R.color.income))
        })
        return row
    }

    private fun budgetRow(name: String, icon: String, iconColor: Int, spent: Long, limit: Long): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply {
                text = "$icon $name"
                textSize = 14f
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@MainActivity).apply {
                text = "${Format.money(spent, symbol)} / ${Format.money(limit, symbol)}"
                textSize = 12f
                setTextColor(color(R.color.subtext))
            })
        }
        row.addView(head)
        row.addView(progressBar(this, if (limit > 0) spent.toFloat() / limit else 0f, iconColor))
        return row
    }

    // ── Transactions tab ────────────────────────────────────────────────────
    private fun buildTransactionsTab(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(monthNav(this, month, { month = Format.addMonths(month, -1); showTab(1) }, { month = Format.addMonths(month, 1); showTab(1) }))

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(4))
            addView(TextView(this@MainActivity).apply {
                text = "In: ${Format.money(db.sumIncome(month), symbol)}"
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.income))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Out: ${Format.money(db.sumExpense(month), symbol)}"
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.expense))
            })
        }
        col.addView(summary)

        // Filter chips
        val filters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(filterChip("All", txTypeFilter == null) { txTypeFilter = null; showTab(1) })
            addView(filterChip("Expense", txTypeFilter == TxType.EXPENSE) { txTypeFilter = TxType.EXPENSE; showTab(1) })
            addView(filterChip("Income", txTypeFilter == TxType.INCOME) { txTypeFilter = TxType.INCOME; showTab(1) })
            addView(filterChip("Category ▾", false) { showCategoryPicker() })
            addView(filterChip("Account ▾", false) { showAccountPicker() })
        }
        col.addView(android.widget.HorizontalScrollView(this).apply {
            addView(filters, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            isHorizontalScrollBarEnabled = false
        })

        val categories = db.getAllCategories()
        val accounts = db.getAccounts()
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }
        val rows = db.getTransactions(month, txTypeFilter, txCategoryFilter, txAccountFilter, "")

        if (rows.isEmpty()) {
            col.addView(TextView(this).apply {
                text = "No transactions for ${Format.monthYear(month)}.\nTap + on the Home tab to add one."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
        } else {
            val scrollBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            var lastDate = ""
            rows.forEach { t ->
                if (t.date != lastDate) {
                    lastDate = t.date
                    scrollBody.addView(TextView(this).apply {
                        text = Format.fullDate(t.date)
                        textSize = 12f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        setTextColor(color(R.color.subtext))
                        setPadding(dp(16), dp(10), dp(16), dp(2))
                    })
                }
                val cat = t.categoryId?.let { catMap[it] }
                scrollBody.addView(txRow(t, cat?.name ?: "Uncategorized", cat?.icon ?: "❓", cat?.color ?: color(R.color.subtext), accMap[t.accountId]?.name))
            }
            col.addView(scroll(scrollBody), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        return col
    }

    private fun filterChip(label: String, selected: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            setPadding(dp(12), dp(7), dp(12), dp(7))
            val bg = GradientDrawable().apply {
                cornerRadius = dpf(20f)
                setColor(if (selected) color(R.color.primary) else color(R.color.card))
            }
            background = bg
            setTextColor(if (selected) Color.WHITE else color(R.color.text))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }
        }

    private fun showCategoryPicker() {
        val categories = db.getAllCategories()
        val names = arrayOf("All categories") + categories.map { it.name }
        val icons = arrayOf("📁") + categories.map { it.icon }
        AlertDialogHelper.pick(
            this, "Category filter", names, icons
        ) { index ->
            txCategoryFilter = if (index == 0) null else categories[index - 1].id
            showTab(1)
        }
    }

    private fun showAccountPicker() {
        val accounts = db.getAccounts()
        val names = arrayOf("All accounts") + accounts.map { it.name }
        AlertDialogHelper.pick(this, "Account filter", names) { index ->
            txAccountFilter = if (index == 0) null else accounts[index - 1].id
            showTab(1)
        }
    }

    // ── Stats tab ───────────────────────────────────────────────────────────
    private fun buildStatsTab(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(monthNav(this, statsMonth, { statsMonth = Format.addMonths(statsMonth, -1); showTab(2) }, { statsMonth = Format.addMonths(statsMonth, 1); showTab(2) }))

        val income = db.sumIncome(statsMonth)
        val expense = db.sumExpense(statsMonth)
        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), dp(6))
            addView(statChip("Income", Format.money(income, symbol), color(R.color.income)))
            addView(statChip("Expenses", Format.money(expense, symbol), color(R.color.expense)))
        }
        col.addView(summary)

        col.addView(sectionLabel(this, "Spending by category"))
        val stats = db.getCategoryStats(statsMonth)
        if (stats.isEmpty()) {
            col.addView(TextView(this).apply {
                text = "No expenses this month."
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setTextColor(color(R.color.subtext))
            })
        } else {
            col.addView(DonutChartView(this).apply {
                setStats(stats)
                layoutParams = LinearLayout.LayoutParams(dp(200), dp(200)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            })
            stats.forEach { stat ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                    addView(CircleBadge(this@MainActivity).apply {
                        emoji = stat.categoryIcon
                        badgeColor = stat.categoryColor
                    }, LinearLayout.LayoutParams(dp(36), dp(36)))
                    addView(TextView(this@MainActivity).apply {
                        text = stat.categoryName
                        textSize = 14f
                        setTextColor(color(R.color.text))
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            setPadding(dp(12), 0, 0, 0)
                        }
                    })
                    val total = stats.fold(0L) { acc, s -> acc + s.total }
                    val pct = if (total > 0) stat.total * 100 / total else 0
                    addView(TextView(this@MainActivity).apply {
                        text = "${Format.money(stat.total, symbol)}  $pct%"
                        textSize = 13f
                        setTextColor(color(R.color.subtext))
                    })
                }
                col.addView(row)
            }
        }

        col.addView(sectionLabel(this, "Last 12 months"))
        col.addView(BarChartView(this).apply {
            setSeries(db.getMonthlySeries(statsMonth))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)).apply {
                setMargins(dp(8), 0, dp(8), 0)
            }
        })
        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(this@MainActivity).apply {
                text = "■ Income   "
                setTextColor(color(R.color.income))
                textSize = 12f
            })
            addView(TextView(this@MainActivity).apply {
                text = "■ Expense"
                setTextColor(color(R.color.expense))
                textSize = 12f
            })
        }
        col.addView(legend)
        col.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(24)) })
        return scroll(col)
    }

    // ── Budgets tab ─────────────────────────────────────────────────────────
    private fun buildBudgetsTab(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(monthNav(this, budgetMonth, { budgetMonth = Format.addMonths(budgetMonth, -1); showTab(3) }, { budgetMonth = Format.addMonths(budgetMonth, 1); showTab(3) }))

        val rows = db.getBudgetRows(budgetMonth)
        val totalLimit = rows.fold(0L) { acc, r -> acc + r.budget.amount }
        val totalSpent = rows.fold(0L) { acc, r -> acc + r.spent }
        col.addView(TextView(this).apply {
            text = "Total budget: ${Format.money(totalLimit, symbol)}    Spent: ${Format.money(totalSpent, symbol)}"
            textSize = 13f
            setPadding(dp(16), dp(4), dp(16), dp(4))
            setTextColor(color(R.color.subtext))
        })

        if (rows.isEmpty()) {
            col.addView(TextView(this).apply {
                text = "No budgets for ${Format.monthYear(budgetMonth)}.\nTap Add Budget to create one."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
        } else {
            val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            rows.forEach { b ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showBudgetDialog(b.budget.id) }
                    setOnLongClickListener {
                        confirm(this@MainActivity, "Delete budget", "Delete this budget?") {
                            db.deleteBudget(b.budget.id)
                            showTab(3)
                        }
                        true
                    }
                    val head = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(TextView(this@MainActivity).apply {
                            text = "${b.categoryIcon} ${b.categoryName}"
                            textSize = 14f
                            setTextColor(color(R.color.text))
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${Format.money(b.spent, symbol)} / ${Format.money(b.budget.amount, symbol)}" +
                                if (b.overspent) "  ⚠" else ""
                            textSize = 12f
                            setTextColor(if (b.overspent) color(R.color.expense) else color(R.color.subtext))
                        })
                    }
                    addView(head)
                    addView(progressBar(this@MainActivity, b.progress, if (b.overspent) color(R.color.expense) else b.categoryColor))
                }
                body.addView(row)
            }
            col.addView(scroll(body), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        col.addView(Button(this).apply {
            text = "+ Add Budget"
            setOnClickListener { showBudgetDialog(null) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(8), dp(16), dp(16))
            }
        })
        return col
    }

    private fun showBudgetDialog(budgetId: Long?) {
        val categories = db.getCategories(TxType.EXPENSE)
        if (categories.isEmpty()) {
            AlertDialogHelper.toast(this, "Add expense categories first (More → Categories)")
            return
        }
        val existing = budgetId?.let { db.getAllBudgets().firstOrNull { b -> b.id == it } }
        var categoryId = existing?.categoryId ?: categories.first().id
        var amountText = existing?.let { Format.money(it.amount, "") } ?: ""

        val builder = AlertDialogHelper.form(this, if (existing == null) "New Budget" else "Edit Budget")

        builder.addLabel("Category")
        val catNames = categories.map { it.name }.toTypedArray()
        builder.addSpinner(catNames, categories.indexOfFirst { it.id == categoryId }.coerceAtLeast(0)) { idx -> categoryId = categories[idx].id }
        builder.addLabel("Monthly limit")
        builder.addAmountField(amountText) { amountText = it }
        builder.addSave("Save") { dialog ->
            val value = amountText.toDoubleOrNull() ?: 0.0
            if (value <= 0) {
                AlertDialogHelper.toast(this, "Enter a limit greater than zero")
            } else {
                db.insertBudget(
                    com.myexpense.tracker.db.Budget(
                        id = existing?.id ?: 0,
                        categoryId = categoryId,
                        amount = (value * 100).toLong().coerceAtLeast(1),
                        month = existing?.month,
                        isRecurring = existing?.isRecurring ?: true
                    )
                )
                dialog.dismiss()
                showTab(3)
            }
        }
        builder.show()
    }

    // ── Settings tab ────────────────────────────────────────────────────────
    private fun buildSettingsTab(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        col.addView(sectionLabel(this, "Appearance"))
        col.addView(TextView(this).apply {
            text = "Theme"
            textSize = 15f
            setTextColor(color(R.color.text))
            setPadding(dp(16), dp(6), dp(16), dp(2))
        })
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(4))
            val modes = listOf("System" to Prefs.THEME_SYSTEM, "Light" to Prefs.THEME_LIGHT, "Dark" to Prefs.THEME_DARK)
            modes.forEach { (label, mode) ->
                addView(filterChip(label, Prefs.themeMode(this@MainActivity) == mode) {
                    Prefs.setThemeMode(this@MainActivity, mode)
                    recreate()
                })
            }
        }
        col.addView(themeRow)

        col.addView(sectionLabel(this, "Currency"))
        col.addView(TextView(this).apply {
            text = "Display currency: ${Format.money(123456, symbol)}"
            textSize = 15f
            setTextColor(color(R.color.text))
            setPadding(dp(16), dp(6), dp(16), dp(2))
        })
        val curRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(4))
            com.myexpense.tracker.util.CurrencySymbols.all.forEach { s ->
                addView(filterChip(s, symbol == s) {
                    Prefs.setCurrency(this@MainActivity, s)
                    symbol = s
                    showTab(4)
                })
            }
        }
        col.addView(android.widget.HorizontalScrollView(this).apply {
            addView(curRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            isHorizontalScrollBarEnabled = false
        })

        col.addView(sectionLabel(this, "Security"))
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(TextView(this@MainActivity).apply {
                text = "Lock app with device credential\n(fingerprint / PIN / pattern)"
                textSize = 14f
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            val toggle = android.widget.Switch(this@MainActivity).apply {
                isChecked = Prefs.biometricEnabled(this@MainActivity)
                setOnCheckedChangeListener { _, checked ->
                    Prefs.setBiometricEnabled(this@MainActivity, checked)
                    if (checked) {
                        AlertDialogHelper.toast(this@MainActivity, "App will be locked with your device credential")
                    }
                }
            }
            addView(toggle)
        })

        col.addView(sectionLabel(this, "Data"))
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addRow(this@MainActivity, "Accounts", "Manage your accounts") { startActivity(Intent(this@MainActivity, AccountsActivity::class.java)) }
            addRow(this@MainActivity, "Categories", "Manage categories, icons & colours") { startActivity(Intent(this@MainActivity, CategoriesActivity::class.java)) }
            addRow(this@MainActivity, "Search transactions", "Find entries by note or category") { startActivity(Intent(this@MainActivity, SearchActivity::class.java)) }
            addRow(this@MainActivity, "Backup, Restore & Export", "JSON backup, CSV, PDF (local files only)") { startActivity(Intent(this@MainActivity, BackupRestoreActivity::class.java)) }
        })

        col.addView(sectionLabel(this, "About"))
        col.addView(TextView(this).apply {
            text = "MoneyMate – Expense Tracker v1.0\n100% offline • No ads • No accounts • No internet permission\nAll data stays on this device."
            textSize = 13f
            setTextColor(color(R.color.subtext))
            setPadding(dp(16), dp(4), dp(16), dp(8))
        })

        col.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(24)) })
        return scroll(col)
    }
}
