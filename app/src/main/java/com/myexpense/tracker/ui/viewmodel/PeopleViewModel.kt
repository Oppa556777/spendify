package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.data.repository.PersonRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonWithStats(
    val person: Person,
    val transactionCount: Int = 0,
    val net: Long = 0,          // income − expense, minor units
)

data class PeopleUiState(
    val people: List<PersonWithStats> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val stats = MutableStateFlow<Map<Long, Pair<Int, Long>>>(emptyMap())

    val uiState: StateFlow<PeopleUiState> = combine(
        personRepository.observeAll(),
        stats,
        settingsRepository.settings,
    ) { people, stats, settings ->
        PeopleUiState(
            people = people.map { person ->
                val s = stats[person.id]
                PersonWithStats(
                    person = person,
                    transactionCount = s?.first ?: 0,
                    net = s?.second ?: 0,
                )
            },
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            val people = personRepository.getAll()
            val map = people.associate { person ->
                person.id to (
                    transactionRepository.countByPerson(person.id) to
                        transactionRepository.personNet(person.id)
                    )
            }
            stats.value = map
        }
    }

    fun create(name: String, phone: String?) {
        viewModelScope.launch {
            personRepository.save(name, phone, 0xFF3B82F6)
            refreshStats()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            personRepository.delete(id)
            refreshStats()
        }
    }
}
