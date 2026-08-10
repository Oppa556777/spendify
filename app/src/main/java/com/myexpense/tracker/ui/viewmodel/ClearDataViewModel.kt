package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.repository.ClearDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClearDataViewModel @Inject constructor(
    private val clearDataRepository: ClearDataRepository,
) : ViewModel() {

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared

    fun clearAll() {
        viewModelScope.launch {
            clearDataRepository.clearAll()
            _cleared.value = true
        }
    }
}
