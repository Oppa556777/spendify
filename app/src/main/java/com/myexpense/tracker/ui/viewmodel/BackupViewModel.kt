package com.myexpense.tracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.repository.BackupData
import com.myexpense.tracker.data.repository.BackupRepository
import com.myexpense.tracker.data.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val preview: BackupData? = null,
    val previewBusy: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            val ok = backupRepository.exportTo(uri)
            _uiState.value = _uiState.value.copy(busy = false, message = if (ok) "Backup saved successfully" else "Export failed")
        }
    }

    fun previewBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(previewBusy = true, message = null)
            val result = backupRepository.preview(uri)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(previewBusy = false, preview = it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(previewBusy = false, message = "Invalid backup: ${it.message}")
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            val result = backupRepository.importFrom(uri)
            _uiState.value = _uiState.value.copy(
                busy = false,
                preview = null,
                message = if (result.isSuccess) "Restore complete" else "Restore failed: ${result.exceptionOrNull()?.message}",
            )
        }
    }

    fun exportCsv(uri: Uri, month: YearMonth?, symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            val result = exportRepository.exportCsv(uri, month, symbol)
            _uiState.value = _uiState.value.copy(
                busy = false,
                message = result.fold(
                    onSuccess = { "CSV exported: $it transactions" },
                    onFailure = { "CSV export failed: ${it.message}" },
                ),
            )
        }
    }

    fun exportPdf(uri: Uri, month: YearMonth, symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            val result = exportRepository.exportPdf(uri, month, symbol)
            _uiState.value = _uiState.value.copy(
                busy = false,
                message = result.fold(
                    onSuccess = { "PDF report saved ($it transactions)" },
                    onFailure = { "PDF export failed: ${it.message}" },
                ),
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
