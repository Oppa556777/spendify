package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.data.model.RecurringFrequency
import com.myexpense.tracker.data.model.Tag
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.PersonRepository
import com.myexpense.tracker.data.repository.RecurringRuleRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TagRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.utils.toRupees
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.roundToLong

/** Mutable state of the built-in calculator. */
data class CalcState(
    val display: String = "0",
    val acc: Double? = null,
    val pendingOp: Char? = null,
)

data class AddEditTransactionUiState(
    val editingId: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val calc: CalcState = CalcState(),
    val title: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: String = "",
    val tags: Set<Long> = emptySet(),
    val personId: Long? = null,
    val locationName: String? = null,
    val receiptPath: String? = null,
    val isRecurring: Boolean = false,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val interval: Int = 1,
    val recurringEnd: Long? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val people: List<Person> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val currencySymbol: String = "₹",
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
    private val tagRepository: TagRepository,
    private val personRepository: PersonRepository,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val achievementUnlocker: AchievementUnlocker,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val editingId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val presetType: TransactionType = runCatching {
        TransactionType.valueOf(savedStateHandle.get<String>("type") ?: TransactionType.EXPENSE.name)
    }.getOrDefault(TransactionType.EXPENSE)

    /** Pre-selected source account (used when opening a transfer from a detail screen). */
    private val presetFrom: Long = savedStateHandle.get<Long>("from")?.takeIf { it > 0 } ?: -1L

    // ── Form fields ────────────────────────────────────────────────────────
    private val calc = MutableStateFlow(CalcState())
    private val type = MutableStateFlow(presetType)
    private val title = MutableStateFlow("")
    private val categoryId = MutableStateFlow<Long?>(null)
    private val accountId = MutableStateFlow<Long?>(null)
    private val toAccountId = MutableStateFlow<Long?>(null)
    private val note = MutableStateFlow("")
    private val date = MutableStateFlow(LocalDate.now())
    private val time = MutableStateFlow(LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
    private val tags = MutableStateFlow<Set<Long>>(emptySet())
    private val personId = MutableStateFlow<Long?>(null)
    private val locationName = MutableStateFlow<String?>(null)
    private val receiptPath = MutableStateFlow<String?>(null)
    private val isRecurring = MutableStateFlow(false)
    private val frequency = MutableStateFlow(RecurringFrequency.MONTHLY)
    private val interval = MutableStateFlow(1)
    private val recurringEnd = MutableStateFlow<Long?>(null)
    private val saved = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    // ── Grouped flows (combine supports ≤5 sources) ────────────────────────
    private data class FormA(val calc: CalcState, val title: String, val type: TransactionType)
    private data class FormB(
        val categoryId: Long?,
        val accountId: Long?,
        val toAccountId: Long?,
        val note: String,
    )
    private data class FormC(val a: FormA, val b: FormB, val date: LocalDate, val time: String)
    private data class FormD(
        val c: FormC,
        val tags: Set<Long>,
        val personId: Long?,
        val locationName: String?,
        val receiptPath: String?,
    )
    private data class FormE(
        val d: FormD,
        val isRecurring: Boolean,
        val frequency: RecurringFrequency,
        val interval: Int,
        val recurringEnd: Long?,
        val saved: Boolean,
        val error: String?,
    )

    private data class FormE1(
        val d: FormD,
        val isRecurring: Boolean,
        val frequency: RecurringFrequency,
        val interval: Int,
    )

    private data class RefA(
        val categories: List<Category>,
        val accounts: List<Account>,
        val allTags: List<Tag>,
    )
    private data class RefB(
        val people: List<Person>,
        val suggestions: List<String>,
        val currencySymbol: String,
    )

    private val formA = combine(calc, title, type) { c, t, ty -> FormA(c, t, ty) }
    private val formB = combine(categoryId, accountId, toAccountId, note) { c, a, ta, n ->
        FormB(c, a, ta, n)
    }
    private val formC = combine(formA, formB, date, time) { a, b, d, t -> FormC(a, b, d, t) }
    private val formD = combine(formC, tags, personId, locationName, receiptPath) { c, t, p, l, r ->
        FormD(c, t, p, l, r)
    }
    private val formE1 = combine(formD, isRecurring, frequency, interval) { d, ir, f, i ->
        FormE1(d, ir, f, i)
    }
    private val formE = combine(formE1, recurringEnd, saved, error) { e1, re, s, e ->
        FormE(e1.d, e1.isRecurring, e1.frequency, e1.interval, re, s, e)
    }

    private val refA = combine(
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
        tagRepository.observeAll(),
    ) { c, a, t -> RefA(c, a, t) }

    private val refB = combine(
        personRepository.observeAll(),
        transactionRepository.observeRecentTitles(),
        settingsRepository.settings,
    ) { p, s, st -> RefB(p, s, st.currencySymbol) }

    private val refs = combine(refA, refB) { a, b -> a to b }

    val uiState: StateFlow<AddEditTransactionUiState> = combine(formE, refs) { f, r ->
        AddEditTransactionUiState(
            editingId = editingId,
            type = f.d.c.a.type,
            calc = f.d.c.a.calc,
            title = f.d.c.a.title,
            categoryId = f.d.c.b.categoryId,
            accountId = f.d.c.b.accountId,
            toAccountId = f.d.c.b.toAccountId,
            note = f.d.c.b.note,
            date = f.d.c.date,
            time = f.d.c.time,
            tags = f.d.tags,
            personId = f.d.personId,
            locationName = f.d.locationName,
            receiptPath = f.d.receiptPath,
            isRecurring = f.isRecurring,
            frequency = f.frequency,
            interval = f.interval,
            recurringEnd = f.recurringEnd,
            categories = r.first.categories,
            accounts = r.first.accounts,
            allTags = r.first.allTags,
            people = r.second.people,
            suggestions = r.second.suggestions,
            currencySymbol = r.second.currencySymbol,
            isLoaded = true,
            saved = f.saved,
            error = f.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEditTransactionUiState())

    // ── Edit-mode loading / preset account ─────────────────────────────────
    init {
        if (presetFrom > 0) {
            accountId.value = presetFrom
        }
        if (editingId != 0L) {
            viewModelScope.launch {
                transactionRepository.getById(editingId)?.let { t ->
                    type.value = t.type
                    title.value = t.title
                    calc.value = CalcState(rupeesToDisplay(t.amount))
                    categoryId.value = t.categoryId
                    accountId.value = t.accountId
                    toAccountId.value = t.toAccountId
                    note.value = t.note
                    date.value = t.date
                    time.value = t.time.ifBlank { LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
                    tags.value = t.tags?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
                    personId.value = t.personId
                    locationName.value = t.locationName
                    receiptPath.value = t.receiptImagePath
                    isRecurring.value = t.isRecurring
                    recurringIdForEdit = t.recurringId
                }
            }
        }
    }

    private var recurringIdForEdit: Long? = null

    // ── Calculator ──────────────────────────────────────────────────────────
    fun digit(d: Char) {
        val cur = calc.value
        val newDisplay = if (cur.display == "0") d.toString() else {
            if (cur.display.length < 12) cur.display + d else cur.display
        }
        calc.value = cur.copy(display = newDisplay)
    }

    fun decimalPoint() {
        val cur = calc.value
        if ('.' !in cur.display) calc.value = cur.copy(display = cur.display + ".")
    }

    fun backspace() {
        val cur = calc.value
        val newDisplay = cur.display.dropLast(1).ifEmpty { "0" }
        calc.value = cur.copy(display = newDisplay)
    }

    fun clearAll() {
        calc.value = CalcState()
    }

    fun pressOperator(op: Char) {
        val cur = calc.value
        val operand = cur.display.toDoubleOrNull() ?: 0.0
        val newAcc = if (cur.acc != null && cur.pendingOp != null) {
            applyOp(cur.acc, operand, cur.pendingOp)
        } else {
            operand
        }
        calc.value = CalcState(display = "0", acc = newAcc, pendingOp = op)
    }

    fun equalsPressed() {
        val cur = calc.value
        val operand = cur.display.toDoubleOrNull() ?: 0.0
        if (cur.acc != null && cur.pendingOp != null) {
            val result = applyOp(cur.acc, operand, cur.pendingOp)
            calc.value = CalcState(display = formatCalc(result))
        }
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '×' -> a * b
        '÷' -> if (b == 0.0) a else a / b
        else -> b
    }

    private fun formatCalc(value: Double): String {
        val rounded = (value * 100).roundToLong() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun rupeesToDisplay(amountMinor: Long): String {
        val abs = kotlin.math.abs(amountMinor)
        val rupees = abs / 100
        val paise = abs % 100
        return if (paise == 0L) rupees.toString() else "$rupees.${paise.toString().padStart(2, '0')}"
    }

    // ── Setters ─────────────────────────────────────────────────────────────
    fun setType(t: TransactionType) {
        type.value = t
        val current = categoryId.value
        val stillValid = current != null &&
            uiState.value.categories.any { it.id == current && it.type == t }
        if (!stillValid) categoryId.value = null
        if (t == TransactionType.TRANSFER) {
            categoryId.value = null
            if (toAccountId.value == accountId.value) toAccountId.value = null
        } else {
            toAccountId.value = null
        }
    }

    fun setTitle(value: String) { title.value = value }
    fun setCategoryId(id: Long?) { categoryId.value = id }
    fun setAccountId(id: Long?) {
        accountId.value = id
        if (toAccountId.value == id) toAccountId.value = null
    }
    fun setToAccountId(id: Long?) { toAccountId.value = id }
    fun setNote(n: String) { note.value = n }
    fun setDate(d: LocalDate) { date.value = d }
    fun setTime(t: String) { time.value = t }
    fun toggleTag(id: Long) {
        tags.value = if (id in tags.value) tags.value - id else tags.value + id
    }
    fun setPersonId(id: Long?) { personId.value = id }
    fun setLocation(name: String?) { locationName.value = name?.ifBlank { null } }
    fun setReceipt(path: String?) { receiptPath.value = path }
    fun setRecurring(enabled: Boolean) { isRecurring.value = enabled }
    fun setFrequency(f: RecurringFrequency) { frequency.value = f }
    fun setInterval(value: Int) { interval.value = value.coerceIn(1, 999) }
    fun setRecurringEnd(end: Long?) { recurringEnd.value = end }

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = categoryRepository.save(
                Category(name = name.trim(), type = type.value, icon = "category", color = 0xFF6C63FF)
            )
            categoryId.value = id
        }
    }

    fun createTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = tagRepository.save(name.trim(), 0xFF6C63FF)
            tags.value = tags.value + id
            achievementUnlocker.onTagCreated()
        }
    }

    fun createPerson(name: String, phone: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = personRepository.save(name.trim(), phone, 0xFF3B82F6)
            personId.value = id
        }
    }

    fun clearError() { error.value = null }

    // ── Save ────────────────────────────────────────────────────────────────
    fun save() {
        val st = uiState.value

        // Evaluate any pending calculator expression.
        val c = st.calc
        val operand = c.display.toDoubleOrNull() ?: 0.0
        val amount = if (c.acc != null && c.pendingOp != null) {
            applyOp(c.acc, operand, c.pendingOp)
        } else {
            operand
        }
        if (amount <= 0) {
            error.value = "Enter an amount greater than zero"
            return
        }

        val account = st.accountId ?: st.accounts.firstOrNull()?.id
        if (account == null) {
            error.value = "Add an account first"
            return
        }

        val isTransfer = st.type == TransactionType.TRANSFER
        val category = if (isTransfer) {
            null
        } else {
            st.categoryId ?: st.categories.firstOrNull { it.type == st.type }?.id
        }
        if (!isTransfer && category == null) {
            error.value = "Select a category"
            return
        }
        val toAccount = st.toAccountId
        if (isTransfer) {
            if (toAccount == null) {
                error.value = "Select a destination account"
                return
            }
            if (toAccount == account) {
                error.value = "Destination account must differ from the source"
                return
            }
        }

        val amountMinor = (amount * 100).roundToLong().coerceAtLeast(1)
        val defaultTitle = when {
            isTransfer -> "Transfer"
            st.type == TransactionType.INCOME -> "Income"
            else -> "Expense"
        }
        val finalTitle = st.title.ifBlank { defaultTitle }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Recurring rule (transfers don't create rules).
            val ruleId = if (st.isRecurring && !isTransfer) {
                recurringRuleRepository.insert(
                    RecurringRuleEntity(
                        title = finalTitle,
                        amount = amountMinor.toRupees(),
                        type = st.type,
                        categoryId = requireNotNull(category),
                        accountId = account,
                        frequency = st.frequency,
                        interval = st.interval.coerceAtLeast(1),
                        startDate = now,
                        endDate = st.recurringEnd,
                    )
                )
            } else {
                recurringIdForEdit
            }

            val transaction = Transaction(
                id = editingId,
                title = finalTitle,
                amount = amountMinor,
                type = st.type,
                categoryId = category,
                accountId = account,
                toAccountId = toAccount,
                note = st.note,
                date = st.date,
                time = st.time,
                tags = st.tags.sorted().joinToString(",").ifEmpty { null },
                personId = st.personId,
                locationName = st.locationName,
                receiptImagePath = st.receiptPath,
                isRecurring = st.isRecurring,
                recurringId = ruleId,
            )
            transactionRepository.save(transaction)
            achievementUnlocker.onTransactionSaved(transaction)

            // Keep account balances live.
            when (st.type) {
                TransactionType.EXPENSE -> accountRepository.adjustBalance(account, -amountMinor)
                TransactionType.INCOME -> accountRepository.adjustBalance(account, amountMinor)
                TransactionType.TRANSFER -> {
                    accountRepository.adjustBalance(account, -amountMinor)
                    accountRepository.adjustBalance(requireNotNull(toAccount), amountMinor)
                }
            }

            saved.value = true
        }
    }
}
