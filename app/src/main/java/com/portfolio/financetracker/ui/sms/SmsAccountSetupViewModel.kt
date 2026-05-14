package com.portfolio.financetracker.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.core.sms.SmsInboxReader
import com.portfolio.financetracker.core.worker.SmsHistorySyncWorker
import com.portfolio.financetracker.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmsSetupUiState(
    val isScanning: Boolean = false,
    val discoveredSenders: List<SmsInboxReader.DiscoveredSender> = emptyList(),
    val selectedAddresses: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val hasScanned: Boolean = false
)

@HiltViewModel
class SmsAccountSetupViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsSetupUiState())
    val uiState: StateFlow<SmsSetupUiState> = _uiState.asStateFlow()

    val isSmsEnabled: StateFlow<Boolean> = dataStoreManager.isSmsTrackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val trackedSenders: StateFlow<Set<String>> = dataStoreManager.trackedSmsSenders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Scan the SMS inbox and show discovered bank senders */
    fun scanInbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            try {
                val discovered = SmsInboxReader.discoverBankSenders(context)
                // Pre-select senders that are already tracked
                val alreadyTracked = dataStoreManager.trackedSmsSenders
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet()).value
                _uiState.update {
                    it.copy(
                        isScanning        = false,
                        discoveredSenders = discovered,
                        selectedAddresses = alreadyTracked,
                        hasScanned        = true,
                        errorMessage      = if (discovered.isEmpty())
                            "No bank SMS found in your inbox. Make sure you have received bank notifications."
                        else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isScanning = false, errorMessage = "Scan failed: ${e.message}")
                }
            }
        }
    }

    fun toggleSender(address: String) {
        _uiState.update { state ->
            val current = state.selectedAddresses.toMutableSet()
            if (current.contains(address)) current.remove(address) else current.add(address)
            state.copy(selectedAddresses = current)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedAddresses = state.discoveredSenders.map { it.address }.toSet())
        }
    }

    fun clearAll() {
        _uiState.update { it.copy(selectedAddresses = emptySet()) }
    }

    /** Save the user's selection and trigger historical sync */
    fun saveAndSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val selected = _uiState.value.selectedAddresses
                dataStoreManager.setTrackedSmsSenders(selected)
                dataStoreManager.setSmsTrackingEnabled(selected.isNotEmpty())

                // Trigger historical backfill in background
                if (selected.isNotEmpty()) {
                    SmsHistorySyncWorker.enqueue(context)
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
