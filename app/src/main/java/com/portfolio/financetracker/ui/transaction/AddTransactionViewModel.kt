package com.portfolio.financetracker.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.InvalidTransactionException
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
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
                            id = transaction.id,
                            amount = transaction.amount.toString(),
                            category = transaction.category,
                            note = transaction.note,
                            type = transaction.type,
                            date = transaction.date,
                            receiptPath = transaction.receiptPath,
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
                _state.value = state.value.copy(receiptPath = event.path)
            }
            is AddTransactionEvent.ChangedRecurring -> {
                _state.value = state.value.copy(recurringPeriod = event.period)
            }
            is AddTransactionEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    state.value.id?.let { id ->
                        val transaction = transactionUseCases.getTransaction(id)
                        transaction?.let {
                            transactionUseCases.deleteTransaction(it)
                            _eventFlow.emit(UiEvent.SaveSuccess)
                        }
                    }
                }
            }
            is AddTransactionEvent.SaveTransaction -> {
                viewModelScope.launch {
                    try {
                        transactionUseCases.addTransaction(
                            Transaction(
                                id = state.value.id ?: 0,
                                amount = state.value.amount.toDoubleOrNull() ?: 0.0,
                                category = state.value.category,
                                note = state.value.note,
                                date = state.value.date,
                                type = state.value.type,
                                receiptPath = state.value.receiptPath,
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
