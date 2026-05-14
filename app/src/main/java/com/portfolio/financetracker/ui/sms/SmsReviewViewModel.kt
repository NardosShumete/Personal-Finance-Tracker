package com.portfolio.financetracker.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository
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

data class SmsReviewUiState(
    val isSyncing: Boolean = false,
    val syncResult: String? = null,   // "Imported 12 transactions" or error
    val selectedTransaction: Transaction? = null  // shown in approval card
)

@HiltViewModel
class SmsReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsReviewUiState())
    val uiState: StateFlow<SmsReviewUiState> = _uiState.asStateFlow()

    /** Live list of pending SMS transactions */
    val pendingTransactions: StateFlow<List<Transaction>> =
        repository.getPendingTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge count for nav item */
    val pendingCount: StateFlow<Int> =
        repository.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Confirm a transaction — moves it from pending to confirmed */
    fun confirm(transaction: Transaction) {
        viewModelScope.launch {
            repository.confirmTransaction(transaction.id)
            // If this was the selected card, dismiss it
            if (_uiState.value.selectedTransaction?.id == transaction.id) {
                _uiState.update { it.copy(selectedTransaction = null) }
            }
        }
    }

    /** Confirm with edits — user changed category/note before confirming */
    fun confirmWithEdits(original: Transaction, edited: Transaction) {
        viewModelScope.launch {
            // Insert the edited version (replaces original via REPLACE strategy)
            repository.insertTransaction(edited.copy(isPending = false))
            if (_uiState.value.selectedTransaction?.id == original.id) {
                _uiState.update { it.copy(selectedTransaction = null) }
            }
        }
    }

    /** Reject — delete the pending transaction entirely */
    fun reject(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            if (_uiState.value.selectedTransaction?.id == transaction.id) {
                _uiState.update { it.copy(selectedTransaction = null) }
            }
        }
    }

    /** Show the approval card for a specific transaction */
    fun selectForReview(transaction: Transaction) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    fun dismissApprovalCard() {
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    /** Trigger historical SMS sync from the device inbox */
    fun syncHistory() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncResult = null) }
            try {
                val count = repository.syncSmsHistory(context)
                _uiState.update {
                    it.copy(
                        isSyncing  = false,
                        syncResult = if (count > 0) "Imported $count transactions for review"
                                     else "No new transactions found"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSyncing = false, syncResult = "Sync failed: ${e.message}")
                }
            }
        }
    }

    fun clearSyncResult() {
        _uiState.update { it.copy(syncResult = null) }
    }
}
