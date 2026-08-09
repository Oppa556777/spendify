package com.myexpense.tracker.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Budget
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** JSON snapshot of the whole database (local file only, no cloud). */
data class BackupData(
    val version: Int = 1,
    val exportedAt: String = java.time.OffsetDateTime.now().toString(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(YearMonth::class.java, YearMonthAdapter())
        .setPrettyPrinting()
        .create()

    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val data = BackupData(
                categories = categoryRepository.getAll(),
                accounts = accountRepository.getAll(),
                transactions = transactionRepository.observeAll().first(),
                budgets = budgetRepository.getAll(),
            )
            val json = gson.toJson(data)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
            } ?: return@withContext false
            true
        }.getOrDefault(false)
    }

    /** Restores a full backup. Replaces everything. */
    suspend fun importFrom(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: error("Cannot read file")
            val data = gson.fromJson(json, BackupData::class.java)
            require(data.categories != null) { "Invalid backup file" }

            transactionRepository.deleteAll()
            categoryRepository.deleteAll()
            accountRepository.deleteAll()
            budgetRepository.deleteAll()

            categoryRepository.insertAll(data.categories)
            accountRepository.insertAll(data.accounts)
            transactionRepository.insertAll(data.transactions)
            budgetRepository.insertAll(data.budgets)
            data
        }
    }

    /** Reads a backup file without applying it (used for preview). */
    suspend fun preview(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: error("Cannot read file")
            val data = gson.fromJson(json, BackupData::class.java)
            require(data.categories != null) { "Invalid backup file" }
            data
        }
    }
}

class LocalDateAdapter : com.google.gson.TypeAdapter<LocalDate>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }

    override fun read(input: com.google.gson.stream.JsonReader): LocalDate? {
        val s = input.nextString() ?: return null
        return if (s.isEmpty()) null else LocalDate.parse(s)
    }
}

class YearMonthAdapter : com.google.gson.TypeAdapter<YearMonth>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: YearMonth?) {
        out.value(value?.toString())
    }

    override fun read(input: com.google.gson.stream.JsonReader): YearMonth? {
        val s = input.nextString() ?: return null
        return if (s.isEmpty()) null else YearMonth.parse(s)
    }
}
