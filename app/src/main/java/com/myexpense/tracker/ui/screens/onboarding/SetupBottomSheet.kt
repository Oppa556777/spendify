package com.myexpense.tracker.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myexpense.tracker.data.model.AccountType
import com.myexpense.tracker.ui.theme.NunitoFamily
import com.myexpense.tracker.utils.TopCurrencies

/**
 * One-time setup sheet shown after the last onboarding page:
 * 1. Currency selector (top 20, default INR ₹)
 * 2. Primary account name + type
 * 3. Starting balance
 * 4. Fingerprint security toggle
 * 5. Done → saved to Room + DataStore, onboarding marked complete
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupBottomSheet(
    onDismiss: () -> Unit,
    onDone: (currency: String, accountName: String, accountType: AccountType, balanceMinor: Long, biometric: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var currency by remember { mutableStateOf("₹") }
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.CASH) }
    var balance by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Set up MoneyMate",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "One-time setup to personalise your tracker. Everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── 1. Currency ────────────────────────────────────────────────
            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TopCurrencies, key = { it.code }) { info ->
                    CurrencyChip(
                        label = "${info.symbol} ${info.code}",
                        selected = currency == info.symbol,
                        onClick = { currency = info.symbol },
                    )
                }
            }

            // ── 2. Primary account ─────────────────────────────────────────
            Text(
                text = "What is your primary account?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account name") },
                placeholder = { Text("e.g. SBI Bank, Cash, Wallet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountType.entries.forEach { type ->
                    FilterChip(
                        selected = accountType == type,
                        onClick = { accountType = type },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── 3. Starting balance ────────────────────────────────────────
            Text(
                text = "Starting balance?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // ── 4. Fingerprint security ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable fingerprint security?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Lock the app with your fingerprint or device credential.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = biometric, onCheckedChange = { biometric = it })
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // ── 5. Done ────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (accountName.isBlank()) {
                        error = "Enter a name for your primary account."
                        return@Button
                    }
                    val minor = ((balance.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceAtLeast(0)
                    onDone(currency, accountName.trim(), accountType, minor, biometric)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "Done",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CurrencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
