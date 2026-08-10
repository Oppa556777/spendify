package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.AssetType
import com.myexpense.tracker.data.repository.AssetRepository
import com.myexpense.tracker.data.repository.AssetView
import com.myexpense.tracker.data.repository.PortfolioSummary
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetsUiState(
    val assets: List<AssetView> = emptyList(),
    val summary: PortfolioSummary = PortfolioSummary(),
    val currencySymbol: String = "$",
)

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AssetsUiState> = combine(
        assetRepository.observePortfolio(),
        settingsRepository.settings,
    ) { (assets, summary), settings ->
        AssetsUiState(assets = assets, summary = summary, currencySymbol = settings.currencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetsUiState())

    fun save(
        id: Long,
        name: String,
        type: AssetType,
        quantity: Double,
        buyPriceMinor: Long,
        currentPriceMinor: Long,
        note: String?,
    ) {
        viewModelScope.launch {
            assetRepository.save(id, name, type, quantity, buyPriceMinor, currentPriceMinor, note)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { assetRepository.delete(id) }
    }
}
