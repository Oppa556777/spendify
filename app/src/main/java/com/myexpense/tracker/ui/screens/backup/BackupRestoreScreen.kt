package com.myexpense.tracker.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myexpense.tracker.ui.viewmodel.BackupViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val currentMonth = remember { YearMonth.now() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var pendingRestoreUri by remember { androidx.compose.runtime.mutableStateOf<Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.previewBackup(it)
            pendingRestoreUri = it
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(it, null, "$") } }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { viewModel.exportPdf(it, currentMonth, "$") } }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Backup & Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "All backups and exports are written to local files you choose. Nothing ever leaves your device — no internet, no cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                ActionCard(
                    icon = Icons.Filled.FileDownload,
                    title = "Full backup (JSON)",
                    subtitle = "Everything: categories, accounts, transactions, budgets",
                    actionLabel = "Export",
                    onClick = {
                        exportBackupLauncher.launch("moneymate-backup-${java.time.LocalDate.now()}.json")
                    },
                )
            }

            item {
                ActionCard(
                    icon = Icons.Filled.FileUpload,
                    title = "Restore backup",
                    subtitle = "Replace current data with a backup file",
                    actionLabel = "Choose file",
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                )
            }

            item {
                ActionCard(
                    icon = Icons.Filled.TableChart,
                    title = "Export CSV",
                    subtitle = "All transactions, openable in Excel/Sheets",
                    actionLabel = "Export",
                    onClick = { csvLauncher.launch("moneymate-transactions.csv") },
                )
            }

            item {
                ActionCard(
                    icon = Icons.Filled.PictureAsPdf,
                    title = "PDF report",
                    subtitle = "Monthly summary for $currentMonth",
                    actionLabel = "Export",
                    onClick = { pdfLauncher.launch("moneymate-report-$currentMonth.pdf") },
                )
            }

            if (state.busy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("  Working…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.preview?.let { preview ->
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Backup preview", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Created: ${preview.exportedAt.take(19).replace('T', ' ')}\n" +
                                    "• ${preview.categories.size} categories\n" +
                                    "• ${preview.accounts.size} accounts\n" +
                                    "• ${preview.transactions.size} transactions\n" +
                                    "• ${preview.budgets.size} budgets",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = { pendingRestoreUri?.let(viewModel::restoreBackup) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Restore this backup")
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}
