package com.myexpense.tracker.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.utils.DateUtils
import com.myexpense.tracker.utils.MoneyFormatter
import com.opencsv.CSVWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * Writes a CSV of all transactions (or a single month when [month] is given).
     * Uses a UTF-8 BOM so Excel opens it correctly.
     */
    suspend fun exportCsv(
        uri: Uri,
        month: YearMonth?,
        symbol: String,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = categoryRepository.getAll().associateBy { it.id }
            val accounts = accountRepository.getActive().associateBy { it.id }
            val transactions = if (month != null) {
                transactionRepository.observeFiltered(month, null, null, null, "").first()
            } else {
                transactionRepository.observeAll().first()
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = CSVWriter(
                    OutputStreamWriter(stream, Charsets.UTF_8),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END
                )
                // BOM for Excel compatibility
                writer.writeNext(arrayOf("\uFEFF"))
                writer.writeNext(arrayOf("Date", "Type", "Amount", "Currency", "Category", "Account", "Note"))
                transactions.forEach { t ->
                    val category = t.categoryId?.let(categories::get)
                    val account = t.accountId?.let(accounts::get)
                    writer.writeNext(
                        arrayOf(
                            t.date.toString(),
                            t.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            MoneyFormatter.format(t.amount),
                            symbol,
                            category?.name ?: "",
                            account?.name ?: "",
                            t.note,
                        )
                    )
                }
                writer.close()
            } ?: error("Cannot open file")
            transactions.size
        }
    }

    /** Generates a PDF report (summary + transaction list) for the given month. */
    suspend fun exportPdf(
        uri: Uri,
        month: YearMonth,
        symbol: String,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = categoryRepository.getAll().associateBy { it.id }
            val accounts = accountRepository.getActive().associateBy { it.id }
            val transactions = transactionRepository.observeFiltered(month, null, null, null, "").first()
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            val pageWidth = 595
            val pageHeight = 842
            val margin = 48f
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 94, 32)
                textSize = 22f
                isFakeBoldText = true
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(33, 33, 33)
                textSize = 13f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(60, 60, 60)
                textSize = 11f
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(130, 130, 130)
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(220, 220, 220)
                strokeWidth = 1f
            }

            var y = margin + 10f
            canvas.drawText("MoneyMate – Expense Report", margin, y, titlePaint)
            y += 22f
            canvas.drawText(DateUtils.monthYear(month), margin, y, headerPaint)
            y += 26f

            canvas.drawText("Income", margin, y, bodyPaint)
            canvas.drawText(MoneyFormatter.formatWithSymbol(income, symbol), pageWidth - margin - 100f, y, bodyPaint)
            y += 18f
            canvas.drawText("Expenses", margin, y, bodyPaint)
            canvas.drawText(MoneyFormatter.formatWithSymbol(expense, symbol), pageWidth - margin - 100f, y, bodyPaint)
            y += 18f
            canvas.drawText("Net", margin, y, bodyPaint)
            canvas.drawText(MoneyFormatter.formatSigned(income - expense, symbol), pageWidth - margin - 100f, y, bodyPaint)
            y += 18f
            canvas.drawText("Transactions: ${transactions.size}", margin, y, mutedPaint)
            y += 28f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 18f

            // Table header
            canvas.drawText("Date", margin, y, headerPaint)
            canvas.drawText("Category", margin + 90f, y, headerPaint)
            canvas.drawText("Note", margin + 220f, y, headerPaint)
            canvas.drawText("Amount", pageWidth - margin - 110f, y, headerPaint)
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f

            transactions.forEach { t ->
                if (y > pageHeight - 60f) return@forEach // keep report to one page
                val categoryName = t.categoryId?.let(categories::get)?.name ?: "—"
                val amountText = if (t.type == TransactionType.INCOME) {
                    "+" + MoneyFormatter.formatWithSymbol(t.amount, symbol)
                } else {
                    "-" + MoneyFormatter.formatWithSymbol(t.amount, symbol)
                }
                canvas.drawText(t.date.toString(), margin, y, bodyPaint)
                canvas.drawText(categoryName, margin + 90f, y, bodyPaint)
                canvas.drawText(t.note.take(28), margin + 220f, y, bodyPaint)
                val amountPaint = Paint(bodyPaint).apply {
                    color = if (t.type == TransactionType.EXPENSE) Color.rgb(200, 40, 40) else Color.rgb(40, 120, 60)
                }
                canvas.drawText(amountText, pageWidth - margin - 110f, y, amountPaint)
                y += 18f
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                document.writeTo(stream)
            } ?: error("Cannot open file")
            document.close()
            transactions.size
        }
    }
}
