package com.portfolio.financetracker.ui.transaction

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.core.util.FileHelper
import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.InvalidTransactionException
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    // ApplicationContext is safe to inject — no memory leak risk
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        savedStateHandle.get<Int>("transactionId")?.let { transactionId ->
            if (transactionId != -1) {
                viewModelScope.launch {
                    transactionUseCases.getTransaction(transactionId)?.let { transaction ->
                        _state.value = state.value.copy(
                            id            = transaction.id,
                            amount        = transaction.amount.toString(),
                            category      = transaction.category,
                            note          = transaction.note,
                            type          = transaction.type,
                            date          = transaction.date,
                            receiptPath   = transaction.receiptPath,
                            recurringPeriod = transaction.recurringPeriod
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.EnteredAmount -> {
                _state.value = state.value.copy(amount = event.value)
            }
            is AddTransactionEvent.EnteredCategory -> {
                _state.value = state.value.copy(category = event.value)
            }
            is AddTransactionEvent.EnteredNote -> {
                _state.value = state.value.copy(note = event.value)
            }
            is AddTransactionEvent.ChangedType -> {
                _state.value = state.value.copy(type = event.type)
            }
            is AddTransactionEvent.ChangedDate -> {
                _state.value = state.value.copy(date = event.date)
            }
            is AddTransactionEvent.ChangedReceipt -> {
                // event.path is a content:// URI string from the gallery picker.
                // Copy it to permanent app storage immediately so it survives restarts.
                viewModelScope.launch {
                    _state.value = state.value.copy(isUploadingReceipt = true)

                    val uri          = Uri.parse(event.path)
                    val permanentPath = FileHelper.copyImageToAppStorage(appContext, uri)

                    if (permanentPath != null) {
                        _state.value = state.value.copy(
                            receiptPath      = permanentPath,
                            isUploadingReceipt = false
                        )
                    } else {
                        _state.value = state.value.copy(isUploadingReceipt = false)
                        _eventFlow.emit(UiEvent.ShowSnackbar("Failed to save receipt. Please try again."))
                    }
                }
            }
            is AddTransactionEvent.ChangedRecurring -> {
                _state.value = state.value.copy(recurringPeriod = event.period)
            }
            is AddTransactionEvent.RemoveReceipt -> {
                // Only clear from state. Deletion happens on Save to avoid data loss if user cancels.
                _state.value = state.value.copy(receiptPath = null)
            }
            is AddTransactionEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    state.value.id?.let { id ->
                        val transaction = transactionUseCases.getTransaction(id)
                        transaction?.let {
                            // Clean up the receipt file before deleting the record
                            FileHelper.deleteReceiptFile(it.receiptPath)
                            transactionUseCases.deleteTransaction(it)
                            _eventFlow.emit(UiEvent.SaveSuccess)
                        }
                    }
                }
            }
            is AddTransactionEvent.SaveTransaction -> {
                viewModelScope.launch {
                    try {
                        val isEdit = state.value.id != null
                        if (isEdit) {
                            val original = transactionUseCases.getTransaction(state.value.id!!)
                            if (original != null && original.receiptPath != state.value.receiptPath) {
                                FileHelper.deleteReceiptFile(original.receiptPath)
                            }
                        }
                        
                        transactionUseCases.addTransaction(
                            Transaction(
                                id              = state.value.id ?: 0,
                                amount          = state.value.amount.toDoubleOrNull() ?: 0.0,
                                category        = state.value.category,
                                note            = state.value.note,
                                date            = state.value.date,
                                type            = state.value.type,
                                receiptPath     = state.value.receiptPath,
                                recurringPeriod = state.value.recurringPeriod
                            )
                        )
                        _eventFlow.emit(UiEvent.SaveSuccess)
                    } catch (e: InvalidTransactionException) {
                        _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Couldn't save transaction"))
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }
}
