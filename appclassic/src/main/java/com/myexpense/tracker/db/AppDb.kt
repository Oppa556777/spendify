package com.myexpense.tracker.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Framework-only SQLite layer (SQLiteOpenHelper). Schema mirrors the
 * Room-based implementation in the main `app` module.
 */
class AppDb(context: Context) : SQLiteOpenHelper(context, "moneymate.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                icon TEXT NOT NULL,
                color INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                initialBalance INTEGER NOT NULL DEFAULT 0,
                color INTEGER NOT NULL,
                icon TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                categoryId INTEGER,
                accountId INTEGER,
                note TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                categoryId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                month TEXT,
                isRecurring INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tx_date ON transactions(date)")
        db.execSQL("CREATE INDEX idx_tx_cat ON transactions(categoryId)")
        db.execSQL("CREATE INDEX idx_tx_acc ON transactions(accountId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS categories")
        db.execSQL("DROP TABLE IF EXISTS accounts")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        onCreate(db)
    }

    // ── Categories ──────────────────────────────────────────────────────────
    fun insertCategory(c: Category): Long =
        writableDatabase.insert("categories", null, c.toValues())

    fun updateCategory(c: Category) {
        writableDatabase.update("categories", c.toValues(), "id=?", arrayOf(c.id.toString()))
    }

    fun deleteCategory(id: Long) {
        writableDatabase.delete("categories", "id=?", arrayOf(id.toString()))
        // transactions keep the category reference but it will resolve to "Uncategorized"
    }

    fun getAllCategories(): List<Category> =
        readableDatabase.query("categories", null, null, null, null, null, "sortOrder ASC, name ASC")
            .use { cur -> cur.toCategories() }

    fun getCategories(type: TxType): List<Category> =
        readableDatabase.query(
            "categories", null, "type=?", arrayOf(type.name), null, null, "sortOrder ASC, name ASC"
        ).use { cur -> cur.toCategories() }

    fun getCategory(id: Long): Category? =
        readableDatabase.query("categories", null, "id=?", arrayOf(id.toString()), null, null, null)
            .use { cur -> if (cur.moveToFirst()) cur.toCategory() else null }

    // ── Accounts ────────────────────────────────────────────────────────────
    fun insertAccount(a: Account): Long =
        writableDatabase.insert("accounts", null, a.toValues())

    fun updateAccount(a: Account) {
        writableDatabase.update("accounts", a.toValues(), "id=?", arrayOf(a.id.toString()))
    }

    fun deleteAccount(id: Long) {
        writableDatabase.delete("accounts", "id=?", arrayOf(id.toString()))
    }

    fun getAccounts(): List<Account> =
        readableDatabase.query("accounts", null, null, null, null, null, "name ASC")
            .use { cur -> cur.toAccounts() }

    fun getAccount(id: Long): Account? =
        readableDatabase.query("accounts", null, "id=?", arrayOf(id.toString()), null, null, null)
            .use { cur -> if (cur.moveToFirst()) cur.toAccount() else null }

    /** Account id → live balance (initial + txns). */
    fun getBalances(): Map<Long, Long> {
        val result = HashMap<Long, Long>()
        readableDatabase.rawQuery(
            "SELECT accountId, SUM(CASE WHEN type='INCOME' THEN amount ELSE -amount END) FROM transactions WHERE accountId IS NOT NULL GROUP BY accountId",
            null
        ).use { cur ->
            while (cur.moveToNext()) {
                result[cur.getLong(0)] = cur.getLong(1)
            }
        }
        return result
    }

    fun getTotalBalance(): Long {
        val initial = readableDatabase.rawQuery("SELECT COALESCE(SUM(initialBalance),0) FROM accounts", null)
            .use { if (it.moveToFirst()) it.getLong(0) else 0L }
        val delta = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE -amount END),0) FROM transactions", null
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        return initial + delta
    }

    // ── Transactions ───────────────────────────────────────────────────────
    fun insertTransaction(t: Transaction): Long =
        writableDatabase.insert("transactions", null, t.toValues())

    fun insertTransactions(list: List<Transaction>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            list.forEach { db.insert("transactions", null, it.toValues()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTransaction(t: Transaction) {
        writableDatabase.update("transactions", t.toValues(), "id=?", arrayOf(t.id.toString()))
    }

    fun deleteTransaction(id: Long) {
        writableDatabase.delete("transactions", "id=?", arrayOf(id.toString()))
    }

    fun getTransaction(id: Long): Transaction? =
        readableDatabase.query("transactions", null, "id=?", arrayOf(id.toString()), null, null, null)
            .use { cur -> if (cur.moveToFirst()) cur.toTransaction() else null }

    fun getTransactions(
        month: String?,          // yyyy-MM or null
        type: TxType?,
        categoryId: Long?,
        accountId: Long?,
        query: String
    ): List<Transaction> {
        val where = StringBuilder()
        val args = ArrayList<String>()
        if (month != null) {
            where.append("date LIKE ?")
            args.add("$month%")
        }
        if (type != null) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("type=?")
            args.add(type.name)
        }
        if (categoryId != null) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("categoryId=?")
            args.add(categoryId.toString())
        }
        if (accountId != null) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("accountId=?")
            args.add(accountId.toString())
        }
        if (query.isNotBlank()) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("(note LIKE ? OR categoryId IN (SELECT id FROM categories WHERE name LIKE ?))")
            val like = "%${query.trim()}%"
            args.add(like)
            args.add(like)
        }
        return readableDatabase.query(
            "transactions", null, where.toString(), args.toTypedArray(), null, null,
            "date DESC, id DESC"
        ).use { cur -> cur.toTransactions() }
    }

    fun getRecent(limit: Int): List<Transaction> =
        readableDatabase.query("transactions", null, null, null, null, null, "date DESC, id DESC", limit.toString())
            .use { cur -> cur.toTransactions() }

    fun sumIncome(month: String): Long = sumByType(month, TxType.INCOME)

    fun sumExpense(month: String): Long = sumByType(month, TxType.EXPENSE)

    private fun sumByType(month: String, type: TxType): Long =
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type=? AND date LIKE ?",
            arrayOf(type.name, "$month%")
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }

    /** Expense breakdown per category for a month. */
    fun getCategoryStats(month: String): List<CategoryStat> {
        val rows = readableDatabase.rawQuery(
            """
            SELECT t.categoryId, SUM(t.amount) as total, COUNT(*) as cnt
            FROM transactions t
            WHERE t.type='EXPENSE' AND t.date LIKE ?
            GROUP BY t.categoryId ORDER BY total DESC
            """.trimIndent(),
            arrayOf("$month%")
        ).use { cur ->
            val list = ArrayList<Triple<Long?, Long, Int>>()
            while (cur.moveToNext()) {
                val cid = if (cur.isNull(0)) null else cur.getLong(0)
                list.add(Triple(cid, cur.getLong(1), cur.getInt(2)))
            }
            list
        }
        val catMap = getAllCategories().associateBy { it.id }
        return rows.map { (cid, total, cnt) ->
            val c = cid?.let { catMap[it] }
            CategoryStat(
                categoryId = cid,
                total = total,
                count = cnt,
                categoryName = c?.name ?: "Uncategorized",
                categoryIcon = c?.icon ?: "❓",
                categoryColor = c?.color ?: 0xFF9E9E9E.toInt()
            )
        }
    }

    /** Monthly income/expense for the last 12 months ending at [month]. */
    fun getMonthlySeries(month: String): List<Pair<String, Pair<Long, Long>>> {
        // month = "yyyy-MM"
        val ym = month.split("-")
        val y = ym[0].toInt()
        val m = ym[1].toInt()
        val result = ArrayList<Pair<String, Pair<Long, Long>>>()
        val db = readableDatabase
        for (off in 11 downTo 0) {
            var yy = y
            var mm = m - off
            while (mm <= 0) { mm += 12; yy-- }
            val key = String.format(Locale.US, "%04d-%02d", yy, mm)
            val income = db.rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='INCOME' AND date LIKE ?",
                arrayOf("$key%")
            ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
            val expense = db.rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='EXPENSE' AND date LIKE ?",
                arrayOf("$key%")
            ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
            result.add(key to (income to expense))
        }
        return result
    }

    // ── Budgets ─────────────────────────────────────────────────────────────
    fun insertBudget(b: Budget): Long = writableDatabase.insert("budgets", null, b.toValues())

    fun updateBudget(b: Budget) {
        writableDatabase.update("budgets", b.toValues(), "id=?", arrayOf(b.id.toString()))
    }

    fun deleteBudget(id: Long) {
        writableDatabase.delete("budgets", "id=?", arrayOf(id.toString()))
    }

    fun getBudgets(month: String): List<Budget> =
        readableDatabase.query(
            "budgets", null, "isRecurring=1 OR month=?", arrayOf(month), null, null, "amount DESC"
        ).use { cur -> cur.toBudgets() }

    fun getAllBudgets(): List<Budget> =
        readableDatabase.query("budgets", null, null, null, null, null, "id ASC")
            .use { cur -> cur.toBudgets() }

    /** Budgets joined with category + spent for the month. */
    fun getBudgetRows(month: String): List<BudgetRow> {
        val catMap = getAllCategories().associateBy { it.id }
        val spentMap = getCategoryStats(month).associate { it.categoryId to it.total }
        return getBudgets(month).map { b ->
            val c = catMap[b.categoryId]
            BudgetRow(
                budget = b,
                categoryName = c?.name ?: "Category",
                categoryIcon = c?.icon ?: "❓",
                categoryColor = c?.color ?: 0xFF4CAF50.toInt(),
                spent = spentMap[b.categoryId] ?: 0L
            )
        }
    }

    // ── Backup / wipe ───────────────────────────────────────────────────────
    fun clearAll() {
        val db = writableDatabase
        db.execSQL("DELETE FROM transactions")
        db.execSQL("DELETE FROM categories")
        db.execSQL("DELETE FROM accounts")
        db.execSQL("DELETE FROM budgets")
    }

    // ── Mapping helpers ─────────────────────────────────────────────────────
    private fun Category.toValues() = ContentValues().apply {
        put("name", name)
        put("type", type.name)
        put("icon", icon)
        put("color", color)
        put("sortOrder", sortOrder)
    }

    private fun Account.toValues() = ContentValues().apply {
        put("name", name)
        put("type", type.name)
        put("initialBalance", initialBalance)
        put("color", color)
        put("icon", icon)
    }

    private fun Transaction.toValues() = ContentValues().apply {
        put("type", type.name)
        put("amount", amount)
        if (categoryId != null) put("categoryId", categoryId) else putNull("categoryId")
        if (accountId != null) put("accountId", accountId) else putNull("accountId")
        put("note", note)
        put("date", date)
    }

    private fun Budget.toValues() = ContentValues().apply {
        put("categoryId", categoryId)
        put("amount", amount)
        if (month != null) put("month", month) else putNull("month")
        put("isRecurring", if (isRecurring) 1 else 0)
    }

    private fun Cursor.toCategories(): List<Category> {
        val list = ArrayList<Category>()
        while (moveToNext()) list.add(toCategory())
        return list
    }

    private fun Cursor.toCategory() = Category(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        type = TxType.valueOf(getString(getColumnIndexOrThrow("type"))),
        icon = getString(getColumnIndexOrThrow("icon")),
        color = getInt(getColumnIndexOrThrow("color")),
        sortOrder = getInt(getColumnIndexOrThrow("sortOrder"))
    )

    private fun Cursor.toAccounts(): List<Account> {
        val list = ArrayList<Account>()
        while (moveToNext()) list.add(toAccount())
        return list
    }

    private fun Cursor.toAccount() = Account(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        type = AccountType.valueOf(getString(getColumnIndexOrThrow("type"))),
        initialBalance = getLong(getColumnIndexOrThrow("initialBalance")),
        color = getInt(getColumnIndexOrThrow("color")),
        icon = getString(getColumnIndexOrThrow("icon"))
    )

    private fun Cursor.toTransactions(): List<Transaction> {
        val list = ArrayList<Transaction>()
        while (moveToNext()) list.add(toTransaction())
        return list
    }

    private fun Cursor.toTransaction() = Transaction(
        id = getLong(getColumnIndexOrThrow("id")),
        type = TxType.valueOf(getString(getColumnIndexOrThrow("type"))),
        amount = getLong(getColumnIndexOrThrow("amount")),
        categoryId = if (isNull(getColumnIndexOrThrow("categoryId"))) null else getLong(getColumnIndexOrThrow("categoryId")),
        accountId = if (isNull(getColumnIndexOrThrow("accountId"))) null else getLong(getColumnIndexOrThrow("accountId")),
        note = getString(getColumnIndexOrThrow("note")),
        date = getString(getColumnIndexOrThrow("date"))
    )

    private fun Cursor.toBudgets(): List<Budget> {
        val list = ArrayList<Budget>()
        while (moveToNext()) {
            val monthIdx = getColumnIndexOrThrow("month")
            list.add(
                Budget(
                    id = getLong(getColumnIndexOrThrow("id")),
                    categoryId = getLong(getColumnIndexOrThrow("categoryId")),
                    amount = getLong(getColumnIndexOrThrow("amount")),
                    month = if (isNull(monthIdx)) null else getString(monthIdx),
                    isRecurring = getInt(getColumnIndexOrThrow("isRecurring")) == 1
                )
            )
        }
        return list
    }

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: AppDb(context.applicationContext).also { instance = it }
            }

        fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        fun monthOfIso(dateIso: String): String = dateIso.substring(0, 7)
        fun currentMonth(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
}
