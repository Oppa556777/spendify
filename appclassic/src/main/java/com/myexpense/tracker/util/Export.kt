package com.myexpense.tracker.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.myexpense.tracker.db.Account
import com.myexpense.tracker.db.AccountType
import com.myexpense.tracker.db.AppDb
import com.myexpense.tracker.db.Budget
import com.myexpense.tracker.db.Category
import com.myexpense.tracker.db.Transaction
import com.myexpense.tracker.db.TxType
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

object Export {

    // ── CSV ─────────────────────────────────────────────────────────────────
    fun exportCsv(context: Context, uri: Uri, month: String?, symbol: String): Int {
        val db = AppDb.get(context)
        val catMap = db.getAllCategories().associateBy { it.id }
        val accMap = db.getAccounts().associateBy { it.id }
        val rows = if (month != null) db.getTransactions(month, null, null, null, "") else db.getTransactions(null, null, null, null, "")
        val sb = StringBuilder()
        sb.append("\uFEFF") // BOM for Excel
        sb.append("Date,Type,Amount,Currency,Category,Account,Note\n")
        rows.forEach { t ->
            sb.append(t.date).append(',')
                .append(if (t.type == TxType.INCOME) "Income" else "Expense").append(',')
                .append(Format.money(t.amount, symbol)).append(',')
                .append(symbol).append(',')
                .append(esc(t.categoryId?.let { catMap[it]?.name } ?: "")).append(',')
                .append(esc(t.accountId?.let { accMap[it]?.name } ?: "")).append(',')
                .append(esc(t.note)).append('\n')
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Cannot open file")
        return rows.size
    }

    private fun esc(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s

    // ── PDF ─────────────────────────────────────────────────────────────────
    fun exportPdf(context: Context, uri: Uri, month: String, symbol: String): Int {
        val db = AppDb.get(context)
        val catMap = db.getAllCategories().associateBy { it.id }
        val rows = db.getTransactions(month, null, null, null, "")
        val income = rows.filter { it.type == TxType.INCOME }.fold(0L) { acc, t -> acc + t.amount }
        val expense = rows.filter { it.type == TxType.EXPENSE }.fold(0L) { acc, t -> acc + t.amount }

        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 94, 32); textSize = 22f; isFakeBoldText = true
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33); textSize = 13f; isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 60, 60); textSize = 11f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 130, 130); textSize = 10f
        }
        val linePaint = Paint().apply { color = Color.rgb(220, 220, 220); strokeWidth = 1f }

        var y = margin + 10f
        canvas.drawText("MoneyMate – Expense Report", margin, y, titlePaint)
        y += 22f
        canvas.drawText(Format.monthYear(month), margin, y, headerPaint)
        y += 26f

        canvas.drawText("Income", margin, y, bodyPaint)
        canvas.drawText(Format.money(income, symbol), pageWidth - margin - 100f, y, bodyPaint)
        y += 18f
        canvas.drawText("Expenses", margin, y, bodyPaint)
        canvas.drawText(Format.money(expense, symbol), pageWidth - margin - 100f, y, bodyPaint)
        y += 18f
        canvas.drawText("Net", margin, y, bodyPaint)
        canvas.drawText(Format.signed(income - expense, symbol), pageWidth - margin - 100f, y, bodyPaint)
        y += 18f
        canvas.drawText("Transactions: ${rows.size}", margin, y, mutedPaint)
        y += 28f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        canvas.drawText("Date", margin, y, headerPaint)
        canvas.drawText("Category", margin + 90f, y, headerPaint)
        canvas.drawText("Note", margin + 220f, y, headerPaint)
        canvas.drawText("Amount", pageWidth - margin - 110f, y, headerPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        rows.forEach { t ->
            if (y > pageHeight - 60f) return@forEach
            val catName = t.categoryId?.let { catMap[it]?.name } ?: "—"
            val amountText = if (t.type == TxType.INCOME) {
                "+" + Format.money(t.amount, symbol)
            } else {
                "-" + Format.money(t.amount, symbol)
            }
            canvas.drawText(t.date, margin, y, bodyPaint)
            canvas.drawText(catName, margin + 90f, y, bodyPaint)
            canvas.drawText(t.note.take(28), margin + 220f, y, bodyPaint)
            val amountPaint = Paint(bodyPaint).apply {
                color = if (t.type == TxType.EXPENSE) Color.rgb(200, 40, 40) else Color.rgb(40, 120, 60)
            }
            canvas.drawText(amountText, pageWidth - margin - 110f, y, amountPaint)
            y += 18f
        }
        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        } ?: throw IllegalStateException("Cannot open file")
        document.close()
        return rows.size
    }

    // ── JSON backup ─────────────────────────────────────────────────────────
    fun exportJson(context: Context, uri: Uri): Boolean {
        val db = AppDb.get(context)
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date()))

        val cats = JSONArray()
        db.getAllCategories().forEach { cats.put(it.toJson()) }
        root.put("categories", cats)

        val accs = JSONArray()
        db.getAccounts().forEach { accs.put(it.toJson()) }
        root.put("accounts", accs)

        val txs = JSONArray()
        db.getTransactions(null, null, null, null, "").forEach { txs.put(it.toJson()) }
        root.put("transactions", txs)

        val budgets = JSONArray()
        db.getAllBudgets().forEach { budgets.put(it.toJson()) }
        root.put("budgets", budgets)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        } ?: return false
        return true
    }

    /** Parses a backup file and returns (summaryText, parsedData) or throws. */
    fun parseJson(context: Context, uri: Uri): JSONObject {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalStateException("Cannot read file")
        return JSONObject(text)
    }

    fun applyJson(context: Context, obj: JSONObject) {
        val db = AppDb.get(context)
        db.clearAll()
        val cats = obj.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until cats.length()) {
            db.insertCategory(cats.getJSONObject(i).toCategory())
        }
        val accs = obj.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accs.length()) {
            db.insertAccount(accs.getJSONObject(i).toAccount())
        }
        val txs = obj.optJSONArray("transactions") ?: JSONArray()
        val list = ArrayList<Transaction>()
        for (i in 0 until txs.length()) {
            list.add(txs.getJSONObject(i).toTransaction())
        }
        db.insertTransactions(list)
        val budgets = obj.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgets.length()) {
            db.insertBudget(budgets.getJSONObject(i).toBudget())
        }
    }

    fun backupSummary(obj: JSONObject): String {
        val cats = obj.optJSONArray("categories")?.length() ?: 0
        val accs = obj.optJSONArray("accounts")?.length() ?: 0
        val txs = obj.optJSONArray("transactions")?.length() ?: 0
        val budgets = obj.optJSONArray("budgets")?.length() ?: 0
        val exported = obj.optString("exportedAt", "?")
        return "Created: $exported\n• $cats categories\n• $accs accounts\n• $txs transactions\n• $budgets budgets"
    }

    private fun Category.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("type", type.name)
        put("icon", icon); put("color", color); put("sortOrder", sortOrder)
    }

    private fun Account.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("type", type.name)
        put("initialBalance", initialBalance); put("color", color); put("icon", icon)
    }

    private fun Transaction.toJson() = JSONObject().apply {
        put("id", id); put("type", type.name); put("amount", amount)
        if (categoryId != null) put("categoryId", categoryId)
        if (accountId != null) put("accountId", accountId)
        put("note", note); put("date", date)
    }

    private fun Budget.toJson() = JSONObject().apply {
        put("id", id); put("categoryId", categoryId); put("amount", amount)
        if (month != null) put("month", month)
        put("isRecurring", isRecurring)
    }

    private fun JSONObject.toCategory() = Category(
        id = optLong("id"), name = getString("name"),
        type = TxType.valueOf(getString("type")), icon = optString("icon", "📦"),
        color = optInt("color", 0xFF4CAF50.toInt()), sortOrder = optInt("sortOrder")
    )

    private fun JSONObject.toAccount() = Account(
        id = optLong("id"), name = getString("name"),
        type = try { AccountType.valueOf(getString("type")) } catch (e: Exception) { AccountType.CASH },
        initialBalance = optLong("initialBalance"), color = optInt("color", 0xFF3F51B5.toInt()),
        icon = optString("icon", "💳")
    )

    private fun JSONObject.toTransaction() = Transaction(
        id = optLong("id"), type = TxType.valueOf(getString("type")),
        amount = getLong("amount"),
        categoryId = if (has("categoryId")) getLong("categoryId") else null,
        accountId = if (has("accountId")) getLong("accountId") else null,
        note = optString("note"), date = optString("date")
    )

    private fun JSONObject.toBudget() = Budget(
        id = optLong("id"), categoryId = getLong("categoryId"), amount = getLong("amount"),
        month = if (has("month")) getString("month") else null,
        isRecurring = optBoolean("isRecurring", true)
    )
}
