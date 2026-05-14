package com.portfolio.financetracker.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.core.worker.SmsHistorySyncWorker
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.AddTransactionUseCase
import com.portfolio.financetracker.domain.use_case.DeleteTransactionUseCase
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

data class PendingReviewUiState(
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val editingTransaction: Transaction? = null  // non-null = edit sheet open
)

@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingReviewUiState())
    val uiState: StateFlow<PendingReviewUiState> = _uiState.asStateFlow()

    /** Live list of transactions awaiting user review */
    val pendingTransactions: StateFlow<List<Transaction>> =
        repository.getPendingTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge count for nav item */
    val pendingCount: StateFlow<Int> =
        repository.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Actions ───────────────────────────────────────────────────────────────

    /** User tapped "Confirm" — mark as confirmed, include in totals */
    fun confirm(transaction: Transaction) {
        viewModelScope.launch {
            repository.confirmTransaction(transaction.id)
        }
    }

    /** User tapped "Confirm All" */
    fun confirmAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach {
                repository.confirmTransaction(it.id)
            }
        }
    }

    /** User tapped "Dismiss" — delete the pending transaction entirely */
    fun dismiss(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
        }
    }

    /** User tapped "Edit" — open the edit sheet */
    fun startEdit(transaction: Transaction) {
        _uiState.update { it.copy(editingTransaction = transaction) }
    }

    /** User saved edits — update the transaction and confirm it */
    fun saveEdit(
        original: Transaction,
        newAmount: Double,
        newCategory: String,
        newNote: String,
        newType: TransactionType
    ) {
        viewModelScope.launch {
            try {
                // Delete the pending version
                deleteTransactionUseCase(original)
                // Insert the edited version as confirmed
                addTransactionUseCase(
                    original.copy(
                        amount    = newAmount,
                        category  = newCategory,
                        note      = newNote,
                        type      = newType,
                        isPending = false
                    )
                )
                _uiState.update { it.copy(editingTransaction = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncMessage = "Edit failed: ${e.message}") }
            }
        }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editingTransaction = null) }
    }

    /** Trigger historical SMS sync */
    fun syncHistory() {
        _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
        SmsHistorySyncWorker.enqueue(context)
        // WorkManager is async — we show a message and let the worker update the list
        _uiState.update {
            it.copy(
                isSyncing   = false,
                syncMessage = "Syncing historical SMS in background…"
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}
