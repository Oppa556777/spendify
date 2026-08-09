package com.myexpense.tracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import com.myexpense.tracker.data.repository.SeedRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.navigation.MoneyMateNavHost
import com.myexpense.tracker.ui.theme.MoneyMateTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var seedRepository: SeedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed default categories/account on first launch.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                seedRepository.seedIfNeeded()
            }
        }

        setContent {
            val lockViewModel: LockViewModel = hiltViewModel()
            val settings by lockViewModel.settings.collectAsStateWithLifecycle()

            MoneyMateTheme(
                themeMode = settings.themeMode,
                dynamicColors = settings.dynamicColors,
            ) {
                val locked = remember(settings.biometricEnabled) {
                    mutableStateOf(settings.biometricEnabled)
                }
                if (settings.biometricEnabled && locked.value) {
                    LockScreen(
                        onUnlocked = { locked.value = false },
                    )
                } else {
                    MoneyMateNavHost()
                }
            }
        }
    }
}

/** Thin ViewModel that exposes settings to the activity (avoids manual flow collection). */
@HiltViewModel
class LockViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : androidx.lifecycle.ViewModel() {
    val settings: kotlinx.coroutines.flow.StateFlow<SettingsRepository.Settings> =
        settingsRepository.settings.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsRepository.Settings(),
        )
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var promptLaunched by remember { mutableStateOf(false) }

    val biometricPrompt = remember {
        BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            },
        )
    }

    LaunchedEffect(promptLaunched) {
        if (promptLaunched) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("MoneyMate")
                .setSubtitle("Unlock your expense tracker")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp).size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "MoneyMate is locked",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Use your fingerprint, face or device credential to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { promptLaunched = true }) {
                Text("Unlock")
            }
        }
    }
}
