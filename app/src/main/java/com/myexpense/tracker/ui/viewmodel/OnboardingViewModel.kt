package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.data.repository.AccountRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the post-onboarding setup:
 * - currency + biometric flag → DataStore
 * - primary account (name, type, starting balance) → Room
 * - marks onboarding as complete → DataStore (never shown again)
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun completeSetup(
        currency: String,
        accountName: String,
        accountType: AccountType,
        balanceMinor: Long,
        biometricEnabled: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            settingsRepository.setCurrencySymbol(currency)
            settingsRepository.setBiometricEnabled(biometricEnabled)
            accountRepository.save(
                Account(
                    name = accountName,
                    type = accountType,
                    balance = balanceMinor,
                    currency = currency,
                    color = colorFor(accountType),
                    icon = iconFor(accountType),
                    isDefault = true,
                )
            )
            settingsRepository.setOnboardingComplete()
            onDone()
        }
    }

    private fun colorFor(type: AccountType): Long = when (type) {
        AccountType.BANK -> 0xFF3949AB
        AccountType.CASH -> 0xFF43A047
        AccountType.CREDIT_CARD -> 0xFFD32F2F
        AccountType.WALLET -> 0xFF00897B
        AccountType.SAVINGS -> 0xFF1E88E5
        AccountType.INVESTMENT -> 0xFF6A1B9A
        AccountType.OTHER -> 0xFF546E7A
    }

    private fun iconFor(type: AccountType): String = when (type) {
        AccountType.BANK -> "account_balance"
        AccountType.CASH -> "payments"
        AccountType.CREDIT_CARD -> "credit_card"
        AccountType.WALLET -> "account_balance_wallet"
        AccountType.SAVINGS -> "savings"
        AccountType.INVESTMENT -> "trending_up"
        AccountType.OTHER -> "account_balance_wallet"
    }
}
