package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Transaction> = emptyList(),
    val currencySymbol: String = "$",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val results = query.flatMapLatest { q ->
        transactionRepository.observeFiltered(null, null, null, null, q)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        results,
        settingsRepository.settings,
    ) { q, list, settings ->
        SearchUiState(query = q, results = list, currencySymbol = settings.currencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { query.value = q }

    fun delete(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }
}
