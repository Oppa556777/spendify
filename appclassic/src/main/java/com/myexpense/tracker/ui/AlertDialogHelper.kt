package com.myexpense.tracker.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.myexpense.tracker.R

/** Small helper for building common dialogs without AppCompat. */
object AlertDialogHelper {

    fun toast(activity: Activity, message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    /** Simple list picker. */
    fun pick(activity: Activity, title: String, items: Array<String>, icons: Array<String>? = null, onSelect: (Int) -> Unit) {
        val display = if (icons != null) {
            Array(items.size) { i -> "${icons[i]}  ${items[i]}" }
        } else {
            items
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setItems(display) { _, which -> onSelect(which) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Material-styled edit dialog. */
    fun edit(activity: Activity, title: String, initial: String, onSave: (String) -> Unit) {
        val input = EditText(activity).apply {
            setText(initial)
            hint = "Name"
            isSingleLine = true
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(
                "Save",
                android.content.DialogInterface.OnClickListener { _, _ ->
                    onSave(input.text.toString())
                }
            )
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Builder-style form dialog. */
    fun form(activity: Activity, title: String): FormBuilder = FormBuilder(activity, title)

    class FormBuilder(private val activity: Activity, title: String) {
        private val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
        }
        private val dialog: AlertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(container)
            .create()

        fun addLabel(text: String) {
            container.addView(TextView(activity).apply {
                this.text = text
                textSize = 12f
                setTextColor(activity.color(R.color.primary))
                setPadding(0, activity.dp(12), 0, activity.dp(4))
            })
        }

        fun addSpinner(items: Array<String>, selected: Int, onChange: (Int) -> Unit = {}): Spinner {
            val spinner = Spinner(activity).apply {
                adapter = ArrayAdapter(
                    activity,
                    android.R.layout.simple_spinner_dropdown_item,
                    items
                )
                if (selected in items.indices) setSelection(selected)
            }
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    onChange(position)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
            container.addView(spinner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return spinner
        }

        fun addAmountField(value: String, onChange: (String) -> Unit = {}): EditText {
            val et = EditText(activity).apply {
                setText(value)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                hint = "0.00"
            }
            et.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    onChange(s?.toString() ?: "")
                }
            })
            container.addView(et, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return et
        }

        fun addTextField(value: String, hint: String, onChange: (String) -> Unit = {}): EditText {
            val et = EditText(activity).apply {
                setText(value)
                this.hint = hint
                isSingleLine = true
            }
            et.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    onChange(s?.toString() ?: "")
                }
            })
            container.addView(et, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return et
        }

        fun addView(view: View) {
            container.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        fun addSave(label: String, onSave: (AlertDialog) -> Unit) {
            val button = android.widget.Button(activity).apply {
                text = label
                setOnClickListener { onSave(dialog) }
                gravity = Gravity.CENTER
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, activity.dp(16), 0, activity.dp(8))
            container.addView(button, lp)
        }

        fun show() = dialog.show()
    }
}
