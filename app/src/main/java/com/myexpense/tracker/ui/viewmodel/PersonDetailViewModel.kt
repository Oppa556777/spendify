package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.PersonRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonDetailUiState(
    val person: Person? = null,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val net: Long = 0,
    val currencySymbol: String = "$",
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val personId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val person = personRepository.observeById(personId)
    private val txs = transactionRepository.observeByPerson(personId)
    private val net = kotlinx.coroutines.flow.flow { emit(transactionRepository.personNet(personId)) }

    val uiState: StateFlow<PersonDetailUiState> = combine(
        person,
        txs,
        net,
        categoryRepository.observeAll(),
        settingsRepository.settings,
    ) { p, t, n, cats, settings ->
        PersonDetailUiState(
            person = p,
            transactions = t,
            categories = cats,
            net = n,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.delete(id) }
    }
}
