package com.portfolio.financetracker.ui.banks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BankTransactionsUiState(
    val bankName: String = "",
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class BankTransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bankName: String = savedStateHandle.get<String>("bankName") ?: ""

    val uiState: StateFlow<BankTransactionsUiState> =
        repository.getTransactionsByBank(bankName)
            .map { transactions ->
                val income  = transactions.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
                val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                BankTransactionsUiState(
                    bankName     = bankName,
                    transactions = transactions,
                    totalIncome  = income,
                    totalExpense = expense,
                    balance      = income - expense
                )
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = BankTransactionsUiState(bankName = bankName, isLoading = true)
            )
}
