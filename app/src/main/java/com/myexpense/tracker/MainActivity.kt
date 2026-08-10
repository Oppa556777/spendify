package com.myexpense.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import com.myexpense.tracker.data.model.Transaction
import com.myexpense.tracker.data.model.TransactionType
import com.myexpense.tracker.data.repository.AchievementUnlocker
import com.myexpense.tracker.data.repository.RecurringRuleRepository
import com.myexpense.tracker.data.repository.SeedRepository
import com.myexpense.tracker.data.repository.SettingsRepository
import com.myexpense.tracker.data.repository.TransactionRepository
import com.myexpense.tracker.navigation.MoneyMateNavHost
import com.myexpense.tracker.ui.screens.onboarding.OnboardingFlow
import com.myexpense.tracker.ui.screens.splash.AnimatedSplashScreen
import com.myexpense.tracker.ui.theme.MoneyMateTheme
import com.myexpense.tracker.utils.NotificationHelper
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var seedRepository: SeedRepository
    @Inject lateinit var recurringRuleRepository: RecurringRuleRepository
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var achievementUnlocker: AchievementUnlocker
    @Inject lateinit var notificationHelper: NotificationHelper

    private val recurringChecked = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                seedRepository.seedIfNeeded()
                achievementUnlocker.onAppOpen()

                // Feature 4: recurring automation — check due rules once per launch.
                if (recurringChecked.compareAndSet(false, true)) {
                    val due = recurringRuleRepository.getDueRules(System.currentTimeMillis())
                    if (due.isNotEmpty()) {
                        notificationHelper.showRecurringDue(due.size)
                        dueRulesToAdd = due
                    }
                }
            }
        }

        val widgetDestination = intent.getStringExtra("open_destination")

        setContent {
            val lockViewModel: LockViewModel = hiltViewModel()
            val settings by lockViewModel.settings.collectAsStateWithLifecycle()

            // Feature 1: notification permission on first launch.
            var askedPermission by rememberSaveable { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            MoneyMateTheme(
                themeMode = settings.themeMode,
                dynamicColors = settings.dynamicColors,
            ) {
                LaunchedEffect(Unit) {
                    if (!askedPermission && Build.VERSION.SDK_INT >= 33) {
                        askedPermission = true
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                AppRoot(settings = settings, initialDestination = widgetDestination)
            }
        }
    }

    /** Feature 4: creates the confirmed recurring transactions and marks rules executed. */
    fun confirmRecurring(ruleIds: Set<Long>, onDone: () -> Unit) {
        val due = dueRulesToAdd ?: emptyList()
        if (due.isEmpty()) { onDone(); return }
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            due.filter { it.id in ruleIds }.forEach { rule ->
                val transaction = Transaction(
                    title = rule.title,
                    amount = rule.amount.toMinorUnits(),
                    type = rule.type,
                    categoryId = rule.categoryId,
                    accountId = rule.accountId,
                    date = LocalDate.now(),
                )
                transactionRepository.save(transaction)
                recurringRuleRepository.markExecuted(rule.id, now)
                achievementUnlocker.onTransactionSaved(transaction)
            }
            dueRulesToAdd = null
            onDone()
        }
    }

    companion object {
        /** Due recurring rules awaiting user confirmation (set during startup). */
        @Volatile
        var dueRulesToAdd: List<RecurringRuleEntity>? = null
    }
}

/**
 * Root of the app: splash → onboarding + setup (first launch) → home.
 * A returning user goes straight from splash to home, gated by the biometric
 * lock when enabled. Widget taps can deep-link (e.g. to Reports).
 */
@Composable
private fun AppRoot(settings: SettingsRepository.Settings, initialDestination: String? = null) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val destination by remember { mutableStateOf(initialDestination) }

    when {
        !splashDone -> AnimatedSplashScreen(onFinished = { splashDone = true })

        !settings.onboardingSeen -> OnboardingFlow(onFinished = { /* settings flow flips onboardingSeen → Home */ })

        else -> {
            val locked = remember(settings.biometricEnabled) {
                mutableStateOf(settings.biometricEnabled)
            }
            if (settings.biometricEnabled && locked.value) {
                LockScreen(
                    onUnlocked = { locked.value = false },
                )
            } else {
                MoneyMateNavHost(initialDestination = destination)
                RecurringDueDialog()
            }
        }
    }
}

/** Feature 4: confirm-and-create dialog for due recurring transactions. */
@Composable
private fun RecurringDueDialog() {
    val context = LocalContext.current
    var dueRules by remember { mutableStateOf(MainActivity.dueRulesToAdd ?: emptyList()) }
    var selected by remember { mutableStateOf(MainActivity.dueRulesToAdd?.map { it.id }?.toSet() ?: emptySet()) }
    var busy by remember { mutableStateOf(false) }

    // Re-check shortly after composition in case the startup coroutine
    // populated the due list after the first frame.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        dueRules = MainActivity.dueRulesToAdd ?: emptyList()
        selected = MainActivity.dueRulesToAdd?.map { it.id }?.toSet() ?: emptySet()
    }

    if (dueRules.isEmpty()) return

    AlertDialog(
        onDismissRequest = { MainActivity.dueRulesToAdd = null; dueRules = emptyList() },
        title = { Text("Recurring transactions due") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${dueRules.size} recurring transaction(s) are due today. Add them?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                dueRules.forEach { rule ->
                    val ruleId = rule.id
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ruleId in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + ruleId else selected - ruleId
                            },
                        )
                        Text(
                            text = "${rule.title} · ${
                                (rule.amount.toMinorUnits().let { com.myexpense.tracker.utils.MoneyFormatter.format(it) })
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    val activity = context as? MainActivity
                    if (activity != null) {
                        activity.confirmRecurring(selected) {
                            dueRules = emptyList()
                        }
                    } else {
                        dueRules = emptyList()
                    }
                },
            ) {
                Text("Add selected")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                MainActivity.dueRulesToAdd = null
                dueRules = emptyList()
            }) {
                Text("Not now")
            }
        },
    )
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
            context as FragmentActivity,
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
