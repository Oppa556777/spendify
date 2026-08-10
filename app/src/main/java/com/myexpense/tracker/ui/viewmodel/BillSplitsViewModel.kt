package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Person
import com.myexpense.tracker.data.repository.BillSplitRepository
import com.myexpense.tracker.data.repository.BillSplitView
import com.myexpense.tracker.data.repository.PersonRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillSplitsUiState(
    val splits: List<BillSplitView> = emptyList(),
    val people: List<Person> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class BillSplitsViewModel @Inject constructor(
    private val billSplitRepository: BillSplitRepository,
    private val personRepository: PersonRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<BillSplitsUiState> = combine(
        billSplitRepository.observeAll(),
        personRepository.observeAll(),
        settingsRepository.settings,
    ) { splits, people, settings ->
        BillSplitsUiState(splits = splits, people = people, currencySymbol = settings.currencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillSplitsUiState())

    fun saveSplit(
        title: String,
        totalMinor: Long,
        date: Long,
        note: String?,
        paidByPersonId: Long?,
        members: List<Triple<Long?, String, Long>>,
    ) {
        viewModelScope.launch {
            billSplitRepository.saveSplit(title, totalMinor, date, note, paidByPersonId, members)
        }
    }

    fun togglePaid(memberId: Long, paid: Boolean) {
        viewModelScope.launch { billSplitRepository.togglePaid(memberId, paid) }
    }

    fun deleteSplit(id: Long) {
        viewModelScope.launch { billSplitRepository.deleteSplit(id) }
    }
}
