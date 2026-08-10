package com.myexpense.tracker.ui

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.myexpense.tracker.R
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.TxType
import com.myexpense.tracker.util.Seed

class CategoriesActivity : Activity() {

    private lateinit var db: AppDb
    private var selectedType = TxType.EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeChoice()
        super.onCreate(savedInstanceState)
        db = AppDb.get(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }
        root.addView(toolbar())

        val typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        val expenseBtn = Button(this).apply {
            text = "Expense"
            setOnClickListener { selectedType = TxType.EXPENSE; rebuild(root) }
        }
        val incomeBtn = Button(this).apply {
            text = "Income"
            setOnClickListener { selectedType = TxType.INCOME; rebuild(root) }
        }
        typeRow.addView(expenseBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        typeRow.addView(incomeBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(typeRow)

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "+ Add Category"
            setOnClickListener { showCategoryDialog(null) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(8), dp(16), dp(16))
            }
        })

        setContentView(root)
        rebuild(root)
    }

    private fun rebuild(root: LinearLayout) {
        val body = root.getChildAt(2) as LinearLayout
        body.removeAllViews()
        val cats = db.getCategories(selectedType)
        if (cats.isEmpty()) {
            body.addView(TextView(this).apply {
                text = "No ${selectedType.name.toLowerCase()} categories yet."
                setPadding(dp(24), dp(32), dp(24), dp(8))
                gravity = Gravity.CENTER
                setTextColor(color(R.color.subtext))
            })
            return
        }
        cats.forEach { cat ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener { showCategoryDialog(cat.id) }
                setOnLongClickListener {
                    confirm(this@CategoriesActivity, "Delete category", "Delete \"${cat.name}\"? Transactions keep their entries.") {
                        db.deleteCategory(cat.id)
                        rebuild(root)
                    }
                    true
                }
                addView(CircleBadge(this@CategoriesActivity).apply {
                    emoji = cat.icon
                    badgeColor = cat.color
                }, LinearLayout.LayoutParams(dp(38), dp(38)))
                addView(TextView(this@CategoriesActivity).apply {
                    text = cat.name
                    textSize = 15f
                    setTextColor(color(R.color.text))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setPadding(dp(12), 0, 0, 0)
                    }
                })
            }
            body.addView(row)
        }
    }

    private fun toolbar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(16), dp(8))
            addView(Button(this@CategoriesActivity).apply {
                text = "‹"
                textSize = 20f
                setOnClickListener { finish() }
            })
            addView(TextView(this@CategoriesActivity).apply {
                text = "Categories"
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(color(R.color.text))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setPadding(dp(12), 0, 0, 0)
                }
            })
        }

    private fun showCategoryDialog(categoryId: Long?) {
        val existing = categoryId?.let { db.getCategory(it) }
        var name = existing?.name ?: ""
        var icon = existing?.icon ?: "📦"
        var color = existing?.color ?: Seed.colorPalette.first()

        val builder = AlertDialogHelper.form(this, if (existing == null) "New Category" else "Edit Category")
        builder.addLabel("Name")
        builder.addTextField(name, "e.g. Coffee") { name = it }

        builder.addLabel("Icon")
        val iconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        Seed.emojiIcons.forEach { emoji ->
            iconRow.addView(TextView(this).apply {
                text = emoji
                textSize = 18f
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    icon = emoji
                    AlertDialogHelper.toast(this@CategoriesActivity, "Icon: $emoji")
                }
                background = GradientDrawable().apply {
                    cornerRadius = dpf(10f)
                    setColor(color(R.color.card))
                }
                setPadding(dp(7), dp(4), dp(7), dp(4))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(4)
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
                    AlertDialogHelper.toast(this@CategoriesActivity, "Colour set")
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
            db.insertCategory(
                com.myexpense.tracker.db.Category(
                    id = existing?.id ?: 0,
                    name = name.trim(),
                    type = selectedType,
                    icon = icon,
                    color = color,
                    sortOrder = existing?.sortOrder ?: 0
                )
            )
            dialog.dismiss()
            rebuild(root())
        }
        builder.show()
    }

    private fun root(): LinearLayout =
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0) as? LinearLayout
            ?: LinearLayout(this)
}
