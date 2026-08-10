package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Category
import com.myexpense.tracker.data.model.Subscription
import com.myexpense.tracker.data.model.monthlyFactor
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.CategoryRepository
import com.myexpense.tracker.data.repository.SubscriptionRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val filter: String? = null,             // category name, null = all
    val categoryNames: List<String> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val monthlyTotal: Long = 0,
    val yearlyTotal: Long = 0,
    val activeCount: Int = 0,
    val currencySymbol: String = "$",
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val achievementUnlocker: AchievementUnlocker,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        subscriptionRepository.observeAllWithCategories(),
        filter,
        categoryRepository.observeByType(com.myexpense.tracker.data.model.TransactionType.EXPENSE),
        settingsRepository.settings,
    ) { subs, f, expenseCats, settings ->
        val names = subs.mapNotNull { it.categoryName }.distinct().sorted()
        val filtered = if (f == null) subs else subs.filter { it.categoryName == f }
        val active = filtered.filter { it.isActive }
        val monthly = active.sumOf { (it.amount * it.billingCycle.monthlyFactor).toLong() }
        val yearly = active.sumOf { (it.amount * it.billingCycle.monthlyFactor * 12).toLong() }
        SubscriptionsUiState(
            subscriptions = filtered,
            filter = f,
            categoryNames = names,
            expenseCategories = expenseCats,
            monthlyTotal = monthly,
            yearlyTotal = yearly,
            activeCount = active.size,
            currencySymbol = settings.currencySymbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    fun setFilter(f: String?) { filter.value = f }

    fun save(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionRepository.save(subscription)
            achievementUnlocker.onSubscriptionSaved()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { subscriptionRepository.delete(id) }
    }

    fun toggleActive(id: Long, active: Boolean) {
        viewModelScope.launch { subscriptionRepository.toggleActive(id, active) }
    }

    fun markNoted(id: Long) {
        viewModelScope.launch { subscriptionRepository.markNoted(id) }
    }
}
