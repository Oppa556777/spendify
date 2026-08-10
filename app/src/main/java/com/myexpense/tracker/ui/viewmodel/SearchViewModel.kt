package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.data.model.Tag
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.PersonRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TagRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.utils.SearchHistoryStore
import com.myexpense.tracker.utils.toEpochMillis
import com.myexpense.tracker.utils.toRupees
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class SearchSort(val label: String) { DATE("Date"), AMOUNT("Amount"), CATEGORY("Category") }

data class SearchFilters(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val tagId: Long? = null,
    val personId: Long? = null,
)

data class SearchUiState(
    val query: String = "",
    val results: List<Transaction> = emptyList(),
    val filters: SearchFilters = SearchFilters(),
    val sort: SearchSort = SearchSort.DATE,
    val history: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val people: List<Person> = emptyList(),
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val personRepository: PersonRepository,
    settingsRepository: SettingsRepository,
    private val historyStore: SearchHistoryStore,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters())
    private val sort = MutableStateFlow(SearchSort.DATE)

    private val results = combine(query, filters, sort) { q, f, s -> Triple(q, f, s) }
        .flatMapLatest { (q, f, s) ->
            transactionRepository.searchAdvanced(
                query = q.trim(),
                from = f.from,
                to = f.to,
                categoryId = f.categoryId,
                accountId = f.accountId,
                minAmount = f.minAmount?.toRupees(),
                maxAmount = f.maxAmount?.toRupees(),
                tagId = f.tagId,
                personId = f.personId,
            ).map { list -> sortList(list, s) }
        }

    private data class Ref(
        val categories: List<Category>,
        val accounts: List<Account>,
        val tags: List<Tag>,
        val people: List<Person>,
        val symbol: String,
    )

    private val ref = combine(
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
        tagRepository.observeAll(),
        personRepository.observeAll(),
        settingsRepository.settings,
    ) { c, a, t, p, s -> Ref(c, a, t, p, s.currencySymbol) }

    private data class Core(
        val query: String,
        val results: List<Transaction>,
        val filters: SearchFilters,
        val sort: SearchSort,
    )

    private val core = combine(query, results, filters, sort) { q, r, f, s -> Core(q, r, f, s) }

    val uiState: StateFlow<SearchUiState> = combine(core, ref) { c, ref ->
        SearchUiState(
            query = c.query,
            results = c.results,
            filters = c.filters,
            sort = c.sort,
            history = historyStore.get(),
            categories = ref.categories,
            accounts = ref.accounts,
            tags = ref.tags,
            people = ref.people,
            currencySymbol = ref.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    private fun sortList(list: List<Transaction>, s: SearchSort): List<Transaction> = when (s) {
        SearchSort.DATE -> list.sortedByDescending { it.date.toEpochDay() }
        SearchSort.AMOUNT -> list.sortedByDescending { it.amount }
        SearchSort.CATEGORY -> list.sortedBy { it.categoryId ?: -1L }
    }

    fun setQuery(q: String) { query.value = q }

    fun submit() {
        historyStore.add(query.value)
    }

    fun useHistory(q: String) {
        query.value = q
    }

    fun clearHistory() {
        historyStore.clear()
    }

    fun setFrom(d: LocalDate?) { filters.value = filters.value.copy(from = d) }
    fun setTo(d: LocalDate?) { filters.value = filters.value.copy(to = d) }
    fun setCategory(id: Long?) { filters.value = filters.value.copy(categoryId = id) }
    fun setAccount(id: Long?) { filters.value = filters.value.copy(accountId = id) }
    fun setMin(v: Long?) { filters.value = filters.value.copy(minAmount = v) }
    fun setMax(v: Long?) { filters.value = filters.value.copy(maxAmount = v) }
    fun setTag(id: Long?) { filters.value = filters.value.copy(tagId = id) }
    fun setPerson(id: Long?) { filters.value = filters.value.copy(personId = id) }
    fun setSort(s: SearchSort) { sort.value = s }
    fun clearFilters() { filters.value = SearchFilters() }

    fun delete(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }
}
