package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import com.myexpense.tracker.data.model.RecurringFrequency
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.RecurringRuleRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.utils.toMinorUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringRuleView(
    val rule: RecurringRuleEntity,
    val categoryName: String?,
    val amountMinor: Long,
    val nextDueDate: Long,
)

data class RecurringUiState(
    val rules: List<RecurringRuleView> = emptyList(),
    val categoryNames: Map<Long, String> = emptyMap(),
    val accounts: List<com.myexpense.tracker.data.model.Account> = emptyList(),
    val expenseCategories: List<com.myexpense.tracker.data.model.Category> = emptyList(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: com.myexpense.tracker.data.repository.AccountRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRuleRepository.observeAll(),
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
        settingsRepository.settings,
    ) { rules, cats, accounts, settings ->
        RecurringUiState(
            rules = rules.map { rule ->
                RecurringRuleView(
                    rule = rule,
                    categoryName = rule.categoryId?.let { id -> cats.firstOrNull { it.id == id }?.name },
                    amountMinor = rule.amount.toMinorUnits(),
                    nextDueDate = rule.lastExecuted?.let {
                        it + intervalMillis(rule)
                    } ?: rule.startDate,
                )
            },
            categoryNames = cats.associate { it.id to it.name },
            accounts = accounts,
            expenseCategories = cats.filter { it.type == com.myexpense.tracker.data.model.TransactionType.EXPENSE },
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    private fun intervalMillis(rule: RecurringRuleEntity): Long = when (rule.frequency) {
        RecurringFrequency.DAILY -> 86_400_000L
        RecurringFrequency.WEEKLY -> 7L * 86_400_000L
        RecurringFrequency.MONTHLY -> 30L * 86_400_000L
        RecurringFrequency.YEARLY -> 365L * 86_400_000L
    } * rule.interval.coerceAtLeast(1)

    fun save(
        id: Long,
        title: String,
        amountMinor: Long,
        type: TransactionType,
        categoryId: Long,
        accountId: Long,
        frequency: RecurringFrequency,
        interval: Int,
    ) {
        viewModelScope.launch {
            val existing = recurringRuleRepository.getById(id)
            recurringRuleRepository.insert(
                RecurringRuleEntity(
                    id = id,
                    title = title,
                    amount = amountMinor / 100.0,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    frequency = frequency,
                    interval = interval.coerceAtLeast(1),
                    startDate = existing?.startDate ?: System.currentTimeMillis(),
                    endDate = existing?.endDate,
                    lastExecuted = existing?.lastExecuted,
                    isActive = existing?.isActive ?: true,
                )
            )
        }
    }

    fun toggleActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            recurringRuleRepository.getById(id)?.let {
                recurringRuleRepository.update(it.copy(isActive = active))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { recurringRuleRepository.delete(id) }
    }
}
