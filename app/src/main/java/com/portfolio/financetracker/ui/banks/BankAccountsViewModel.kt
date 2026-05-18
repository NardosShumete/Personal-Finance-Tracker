package com.portfolio.financetracker.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.ui.dashboard.BankBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BankAccountsUiState(
    val accounts: List<BankAccountEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class BankAccountsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    /** Live list of all bank accounts from Room */
    val uiState: StateFlow<BankAccountsUiState> =
        repository.getBankAccounts()
            .map { accounts -> BankAccountsUiState(accounts = accounts) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = BankAccountsUiState(isLoading = true)
            )
}
