package com.markel.flowstate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.backup.BackupRepository
import com.markel.flowstate.core.data.backup.RestoreErrorType
import com.markel.flowstate.core.data.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * States for each backup operation (export / restore).
 * State-machine: IDLE → IN_PROGRESS → SUCCESS / FAILURE.
 */
enum class BackupOperationState { IDLE, IN_PROGRESS, SUCCESS, FAILURE }

/**
 * UI state exposed to [BackupScreen].
 */
data class BackupUiState(
    val exportState: BackupOperationState = BackupOperationState.IDLE,
    val restoreState: BackupOperationState = BackupOperationState.IDLE,
    val restoreError: RestoreErrorType? = null
)

/**
 * One-shot events that the ViewModel sends to the UI layer.
 * Used to trigger the SAF file picker after the JSON is ready.
 */
sealed class BackupEvent {
    /** The JSON has been generated and is ready to be written by the UI. */
    data class ExportReady(val json: String) : BackupEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    /** Reset both operations to IDLE — called when entering the screen. */
    fun resetState() {
        _state.value = BackupUiState()
    }

    // ── Export ────────────────────────────────────────────────────────

    /**
     * Generates the backup JSON and emits [BackupEvent.ExportReady].
     * The UI should launch the SAF CreateDocument picker on that event
     * and write [BackupEvent.ExportReady.json] to the user-chosen URI.
     */
    fun startExport() {
        viewModelScope.launch {
            _state.value = _state.value.copy(exportState = BackupOperationState.IN_PROGRESS)
            try {
                val json = backupRepository.exportToJson()
                _events.emit(BackupEvent.ExportReady(json))
            } catch (_: Exception) {
                _state.value = _state.value.copy(exportState = BackupOperationState.FAILURE)
            }
        }
    }

    /** Call after the UI has successfully written the JSON to a URI. */
    fun onExportSaved() {
        _state.value = _state.value.copy(exportState = BackupOperationState.SUCCESS)
    }

    /** Call when the user cancels the SAF picker. */
    fun onExportCancelled() {
        _state.value = _state.value.copy(exportState = BackupOperationState.IDLE)
    }

    // ── Restore ───────────────────────────────────────────────────────

    /**
     * Validates and applies the backup JSON that the UI read from a SAF
     * file picker result.
     */
    fun restoreData(json: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(restoreState = BackupOperationState.IN_PROGRESS)
            when (val result = backupRepository.restoreFromJson(json)) {
                is RestoreResult.Success ->
                    _state.value = _state.value.copy(restoreState = BackupOperationState.SUCCESS)
                is RestoreResult.Error ->
                    _state.value = _state.value.copy(
                        restoreState = BackupOperationState.FAILURE,
                        restoreError = result.type
                    )
            }
        }
    }
}
