package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditTransactionUiState(
    val editingId: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val amountMinor: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencySymbol: String = "$",
    val isLoaded: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val type = MutableStateFlow(TransactionType.EXPENSE)
    private val title = MutableStateFlow("")
    private val amountMinor = MutableStateFlow("")
    private val categoryId = MutableStateFlow<Long?>(null)
    private val accountId = MutableStateFlow<Long?>(null)
    private val note = MutableStateFlow("")
    private val date = MutableStateFlow(LocalDate.now())
    private val saved = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private data class Form(
        val type: TransactionType,
        val title: String,
        val amountMinor: String,
        val categoryId: Long?,
        val accountId: Long?,
        val note: String,
        val date: LocalDate,
        val saved: Boolean,
        val error: String?,
    )

    private data class Reference(
        val categories: List<Category>,
        val accounts: List<Account>,
        val currencySymbol: String,
    )

    private val form = combine(type, title, amountMinor, categoryId, accountId, note) { t, ti, am, c, a, n ->
        FormPart1(t, ti, am, c, a, n)
    }.let { part1 ->
        combine(part1, date, saved, error) { p1, d, s, e ->
            Form(p1.type, p1.title, p1.amountMinor, p1.categoryId, p1.accountId, p1.note, d, s, e)
        }
    }

    private val reference = combine(
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
        settingsRepository.settings,
    ) { cats, accs, settings ->
        Reference(cats, accs, settings.currencySymbol)
    }

    val uiState: StateFlow<AddEditTransactionUiState> = combine(form, reference) { f, r ->
        AddEditTransactionUiState(
            editingId = editingId,
            type = f.type,
            title = f.title,
            amountMinor = f.amountMinor,
            categoryId = f.categoryId,
            accountId = f.accountId,
            note = f.note,
            date = f.date,
            categories = r.categories,
            accounts = r.accounts,
            currencySymbol = r.currencySymbol,
            isLoaded = true,
            saved = f.saved,
            error = f.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEditTransactionUiState())

    private data class FormPart1(
        val type: TransactionType,
        val title: String,
        val amountMinor: String,
        val categoryId: Long?,
        val accountId: Long?,
        val note: String,
    )

    init {
        if (editingId != 0L) {
            viewModelScope.launch {
                transactionRepository.getById(editingId)?.let { t ->
                    type.value = t.type
                    title.value = t.title
                    amountMinor.value = formatAmountInput(t.amount)
                    categoryId.value = t.categoryId
                    accountId.value = t.accountId
                    note.value = t.note
                    date.value = t.date
                }
            }
        }
    }

    private fun formatAmountInput(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)
        return sign + (abs / 100).toString() + if (abs % 100 != 0L) {
            "." + (abs % 100).toString().padStart(2, '0')
        } else {
            ""
        }
    }

    fun setType(t: TransactionType) {
        type.value = t
        // A category belongs to exactly one type; reset when it no longer matches.
        val current = categoryId.value
        val stillValid = current != null &&
            uiState.value.categories.any { it.id == current && it.type == t }
        if (!stillValid) categoryId.value = null
    }

    fun setTitle(value: String) { title.value = value }
    fun setAmount(raw: String) { amountMinor.value = raw.filter { it.isDigit() || it == '.' } }
    fun setCategoryId(id: Long?) { categoryId.value = id }
    fun setAccountId(id: Long?) { accountId.value = id }
    fun setNote(n: String) { note.value = n }
    fun setDate(d: LocalDate) { date.value = d }

    fun save() {
        val amountText = amountMinor.value.trim()
        val parsed = amountText.toDoubleOrNull() ?: 0.0
        if (parsed <= 0.0) {
            error.value = "Enter an amount greater than zero"
            return
        }
        val state = uiState.value

        // categoryId/accountId are NOT NULL in the schema: default them when unset.
        val category = categoryId.value
            ?: state.categories.firstOrNull { it.type == type.value }?.id
        if (category == null) {
            error.value = "Add a ${type.value.name.lowercase()} category first (More → Categories)"
            return
        }
        val account = accountId.value ?: state.accounts.firstOrNull()?.id
        if (account == null) {
            error.value = "Add an account first (More → Accounts)"
            return
        }

        val minor = (parsed * 100).toLong().coerceAtLeast(1)
        val transaction = Transaction(
            id = editingId,
            title = title.value.trim(),
            amount = minor,
            type = type.value,
            categoryId = category,
            accountId = account,
            note = note.value.trim(),
            date = date.value,
        )
        viewModelScope.launch {
            transactionRepository.save(transaction)
            saved.value = true
        }
    }

    fun clearError() { error.value = null }
}
