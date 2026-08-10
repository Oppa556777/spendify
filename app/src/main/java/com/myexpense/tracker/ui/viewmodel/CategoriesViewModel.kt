package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val error = MutableStateFlow<String?>(null)

    private val categories = combine(selectedType, categoryRepository.observeAll()) { type, all ->
        all.filter { it.type == type }
    }

    val uiState: StateFlow<CategoriesUiState> = combine(selectedType, categories, error) { type, list, err ->
        CategoriesUiState(selectedType = type, categories = list, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun setType(type: TransactionType) { selectedType.value = type }

    fun save(category: Category) {
        viewModelScope.launch {
            categoryRepository.save(category)
            error.value = null
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            val ok = categoryRepository.delete(category.id)
            error.value = if (ok) {
                null
            } else {
                "Cannot delete the last ${category.type.name.lowercase()} category. Create another one first."
            }
        }
    }

    fun clearError() { error.value = null }
}
