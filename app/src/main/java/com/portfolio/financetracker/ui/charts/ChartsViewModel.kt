package com.portfolio.financetracker.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(ChartsState())
    val state: StateFlow<ChartsState> = _state.asStateFlow()

    init {
        getExpensesByCategory()
    }

    private fun getExpensesByCategory() {
        transactionUseCases.getTransactions()
            .onEach { allTransactions ->
                val expenses = allTransactions.filter { it.type == TransactionType.EXPENSE }
                val totalExpense = expenses.sumOf { it.amount }

                val categoryMap = expenses
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                _state.value = ChartsState(
                    categoryExpenses = categoryMap,
                    totalExpense = totalExpense
                )
            }
            .launchIn(viewModelScope)
    }
}
