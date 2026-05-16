package com.portfolio.financetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import com.portfolio.financetracker.domain.repository.BankAccountRepository
import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankAccountViewModel @Inject constructor(
    private val repository: BankAccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val bankAccounts: StateFlow<List<BankAccountEntity>> = repository.getAllBankAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedBankId = MutableStateFlow<Int?>(null)
    val expandedBankId: StateFlow<Int?> = _expandedBankId.asStateFlow()

    init {
        seedDefaultBanks()
    }

    private fun seedDefaultBanks() {
        viewModelScope.launch {
            val defaults = listOf(
                BankAccountEntity(shortName = "CBE", fullName = "Commercial Bank of Ethiopia", smsSenderId = "CBEBirr", colorHex = "#0055A4", isConnected = false),
                BankAccountEntity(shortName = "BOA", fullName = "Bank of Abyssinia", smsSenderId = "BOABank", colorHex = "#FFD700", isConnected = false),
                BankAccountEntity(shortName = "Hibret", fullName = "Cooperative Bank", smsSenderId = "CoopBank", colorHex = "#228B22", isConnected = false),
                BankAccountEntity(shortName = "Dashen", fullName = "Dashen Bank", smsSenderId = "DashenBank", colorHex = "#800000", isConnected = false)
            )
            defaults.forEach { bank ->
                if (repository.getBankAccountByShortName(bank.shortName) == null) {
                    repository.insertBankAccount(bank)
                }
            }
            refreshTotals()
        }
    }

    fun toggleExpand(id: Int) {
        _expandedBankId.update { if (it == id) null else id }
    }

    fun toggleConnect(id: Int) {
        viewModelScope.launch {
            val bank = bankAccounts.value.find { it.id == id }
            bank?.let {
                repository.updateBankAccount(it.copy(isConnected = !it.isConnected))
            }
        }
    }

    fun addBank(shortName: String, fullName: String, smsSenderId: String) {
        viewModelScope.launch {
            val newBank = BankAccountEntity(
                shortName = shortName,
                fullName = fullName,
                smsSenderId = smsSenderId,
                colorHex = "#808080",
                isConnected = false
            )
            repository.insertBankAccount(newBank)
        }
    }

    fun refreshTotals() {
        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactions().first()
            val accounts = bankAccounts.value
            accounts.forEach { account ->
                val bankTransactions = transactions.filter { it.bankName == account.shortName }
                val income = bankTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = bankTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                repository.updateTotals(account.id, income, expense, bankTransactions.size)
            }
        }
    }
}
