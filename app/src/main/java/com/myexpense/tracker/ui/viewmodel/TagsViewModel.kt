package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Tag
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TagRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagWithCount(
    val tag: Tag,
    val count: Int,
)

data class TagsUiState(
    val tags: List<TagWithCount> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val counts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    val uiState: StateFlow<TagsUiState> = combine(
        tagRepository.observeAll(),
        counts,
        settingsRepository.settings,
    ) { tags, counts, settings ->
        TagsUiState(
            tags = tags.map { tag -> TagWithCount(tag, counts[tag.id] ?: 0) },
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    init {
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val tags = tagRepository.getAll()
            val map = tags.associate { tag -> tag.id to transactionRepository.countByTag(tag.id) }
            counts.value = map
        }
    }

    fun create(name: String, color: Long) {
        viewModelScope.launch {
            tagRepository.save(name, color)
            refreshCounts()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            tagRepository.delete(id)
            refreshCounts()
        }
    }
}
