package com.portfolio.financetracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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
            is AddTransactionEvent.SaveTransaction -> {
                viewModelScope.launch {
                    try {
                        transactionUseCases.addTransaction(
                            Transaction(
                                amount = state.value.amount.toDoubleOrNull() ?: 0.0,
                                category = state.value.category,
                                note = state.value.note,
                                date = state.value.date,
                                type = state.value.type
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
