package com.portfolio.financetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    private var allTransactions: List<Transaction> = emptyList()

    init {
        getTransactions()
    }
    
    fun onEvent(event: DashboardEvent) {
        when(event) {
            is DashboardEvent.OnSearchQueryChanged -> {
                _state.value = state.value.copy(searchQuery = event.query)
                updateStateWithFilter()
            }
            is DashboardEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    transactionUseCases.deleteTransaction(event.transaction)
                }
            }
        }
    }

    private fun getTransactions() {
        transactionUseCases.getTransactions()
            .onEach { transactions ->
                allTransactions = transactions
                updateStateWithFilter()
            }
            .launchIn(viewModelScope)
    }
    
    private fun updateStateWithFilter() {
        val filtered = if (_state.value.searchQuery.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter { 
                it.category.contains(_state.value.searchQuery, ignoreCase = true) ||
                it.note.contains(_state.value.searchQuery, ignoreCase = true)
            }
        }
        
        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense
        
        _state.value = _state.value.copy(
            transactions = filtered,
            totalBalance = balance,
            totalIncome = income,
            totalExpense = expense
        )
    }
}
