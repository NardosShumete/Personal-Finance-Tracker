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

    // Drives the delete confirmation dialog — null means no dialog shown
    private val _bankToDelete = MutableStateFlow<BankAccountEntity?>(null)
    val bankToDelete: StateFlow<BankAccountEntity?> = _bankToDelete.asStateFlow()

    init {
        viewModelScope.launch {
            seedDefaultBanks()
            // Reactively refresh bank totals whenever the transaction list changes.
            transactionRepository.getAllTransactions().collect {
                refreshTotals()
            }
        }
    }

    private suspend fun seedDefaultBanks() {
        val defaults = listOf(
            BankAccountEntity(shortName = "CBE",      fullName = "Commercial Bank of Ethiopia", smsSenderId = "CBEBirr",    colorHex = "#0055A4", isConnected = false),
            BankAccountEntity(shortName = "BOA",      fullName = "Bank of Abyssinia",           smsSenderId = "BOABank",    colorHex = "#FFD700", isConnected = false),
            BankAccountEntity(shortName = "Telebirr", fullName = "Telebirr (Ethio Telecom)",    smsSenderId = "Telebirr",   colorHex = "#00A651", isConnected = false),
            BankAccountEntity(shortName = "Hibret",   fullName = "Cooperative Bank of Oromia", smsSenderId = "CoopBank",   colorHex = "#228B22", isConnected = false),
            BankAccountEntity(shortName = "Dashen",   fullName = "Dashen Bank",                 smsSenderId = "DashenBank", colorHex = "#800000", isConnected = false),
            BankAccountEntity(shortName = "Awash",    fullName = "Awash Bank",                  smsSenderId = "AwashBank",  colorHex = "#FF6600", isConnected = false)
        )
        defaults.forEach { bank ->
            // Case-insensitive check — avoids re-seeding if "telebirr" already exists
            val existing = repository.getAllBankAccounts()
                .first()
                .any { it.shortName.equals(bank.shortName, ignoreCase = true) }
            if (!existing) {
                repository.insertBankAccount(bank)
            }
        }
    }

    fun toggleExpand(id: Int) {
        _expandedBankId.update { if (it == id) null else id }
    }

    fun toggleConnect(id: Int) {
        viewModelScope.launch {
            val bank = bankAccounts.value.find { it.id == id }
            bank?.let { repository.updateBankAccount(it.copy(isConnected = !it.isConnected)) }
        }
    }

    fun addBank(shortName: String, fullName: String, smsSenderId: String) {
        viewModelScope.launch {
            repository.insertBankAccount(
                BankAccountEntity(
                    shortName   = shortName.trim(),
                    fullName    = fullName.trim(),
                    smsSenderId = smsSenderId.trim(),
                    colorHex    = "#808080",
                    isConnected = false
                )
            )
        }
    }

    // ── Delete with confirmation ──────────────────────────────────────────────

    /** Called when the user taps the X on a bank card — shows the dialog. */
    fun requestDelete(bank: BankAccountEntity) {
        _bankToDelete.value = bank
    }

    /** Called when the user cancels the confirmation dialog. */
    fun cancelDelete() {
        _bankToDelete.value = null
    }

    /** Called when the user confirms deletion. */
    fun confirmDelete() {
        val bank = _bankToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteBankAccount(bank.id)
            _bankToDelete.value = null
        }
    }

    // ── Totals refresh ────────────────────────────────────────────────────────

    fun refreshTotals() {
        viewModelScope.launch {
            // Only confirmed (non-pending) transactions count toward balances
            val transactions = transactionRepository.getAllTransactions()
                .first()
                .filter { !it.isPending }

            val accounts = bankAccounts.value
            accounts.forEach { account ->
                val bankTxns = transactions.filter {
                    it.bankName.equals(account.shortName, ignoreCase = true)
                }
                val income  = bankTxns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
                val expense = bankTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                // lastKnownBalance = smsBalance from the most recent SMS transaction
                // that actually has a balance field. This is the real account balance
                // as reported by the bank, not a running net calculation.
                val lastKnownBalance = bankTxns
                    .filter { it.smsBalance != null }
                    .maxByOrNull { it.date }
                    ?.smsBalance

                repository.updateTotals(account.id, income, expense, bankTxns.size, lastKnownBalance)
            }
        }
    }
}
