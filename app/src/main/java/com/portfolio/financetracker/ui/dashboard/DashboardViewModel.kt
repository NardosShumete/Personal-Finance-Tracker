package com.portfolio.financetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.repository.BankAccountRepository
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val goalUseCases: GoalUseCases,
    private val bankAccountRepository: BankAccountRepository
) : ViewModel() {

    private val _searchQuery   = MutableStateFlow("")
    private val _selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)

    val pagedTransactions: Flow<PagingData<Transaction>> =
        transactionUseCases.getPagedTransactions().cachedIn(viewModelScope)

    val pendingCount: StateFlow<Int> =
        transactionUseCases.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val state: StateFlow<DashboardState> = combine(
        transactionUseCases.getTransactions(),
        _searchQuery,
        _selectedPeriod,
        goalUseCases.getGoal(
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        ),
        bankAccountRepository.getAllBankAccounts()
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allTransactions = args[0] as List<Transaction>
        val query           = args[1] as String
        val period          = args[2] as SummaryPeriod
        val goal            = args[3] as com.portfolio.financetracker.domain.model.MonthlyGoal?
        val bankAccounts    = args[4] as List<com.portfolio.financetracker.data.local.entity.BankAccountEntity>
        // Only confirmed transactions affect any totals
        val confirmed = allTransactions.filter { !it.isPending }

        // ── Time boundaries ───────────────────────────────────────────────────
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // ── Period-filtered subsets ───────────────────────────────────────────
        val todayTxns = confirmed.filter { it.date >= startOfToday }
        val monthTxns = confirmed.filter { it.date >= startOfMonth }

        // ── All-time totals (used for net balance) ────────────────────────────
        val totalIncome  = confirmed.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val totalExpense = confirmed.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // ── Today totals ──────────────────────────────────────────────────────
        val todayIncome  = todayTxns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val todayExpense = todayTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // ── This month totals ─────────────────────────────────────────────────
        val monthIncome  = monthTxns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val monthExpense = monthTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // ── Per-bank balances (from transactions) ─────────────────────────────
        val periodTxns = when (period) {
            SummaryPeriod.TODAY -> todayTxns
            SummaryPeriod.THIS_MONTH -> monthTxns
            SummaryPeriod.ALL_TIME -> confirmed
        }

        val bankBalances = confirmed
            .filter { it.bankName != null }
            .groupBy { it.bankName!! }
            .mapValues { (bankName, allBankTxns) ->
                // Income and expense should be calculated from the PERIOD transactions
                val periodBankTxns = periodTxns.filter { it.bankName == bankName }
                val bIncome  = periodBankTxns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
                val bExpense = periodBankTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                
                // For the Total Balance, use the most recent smsBalance
                val latestSmsBalance = allBankTxns
                    .sortedByDescending { it.date }
                    .firstNotNullOfOrNull { it.smsBalance }
                    
                // If there's no smsBalance, fall back to historical sum of ALL TIME
                val historicalBalance = allBankTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } - 
                                        allBankTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                                        
                val finalBalance = latestSmsBalance ?: historicalBalance

                BankBalance(name = bankName, balance = finalBalance, income = bIncome, expense = bExpense)
            }

        // ── Total balance: sum real SMS balances from bank accounts ───────────
        // If any bank has a real SMS-reported balance, sum those up.
        // Banks without SMS data contribute their net (income - expense).
        // Manual-only transactions (no bankName) are added on top.
        val bankAccountsTotal = bankAccounts.sumOf { account ->
            account.lastKnownBalance ?: (account.totalIncome - account.totalExpense)
        }

        // Manual transactions not linked to any bank account
        val manualTotal = confirmed
            .filter { it.bankName == null }
            .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }

        // Use bank-account-based total when there are bank accounts with data,
        // otherwise fall back to pure transaction net
        val hasBankData = bankAccounts.any { it.transactionCount > 0 || it.lastKnownBalance != null }
        val totalBalance = if (hasBankData) bankAccountsTotal + manualTotal
                           else totalIncome - totalExpense

        DashboardState(
            totalBalance    = totalBalance,
            totalIncome     = totalIncome,
            totalExpense    = totalExpense,
            todayIncome     = todayIncome,
            todayExpense    = todayExpense,
            monthIncome     = monthIncome,
            monthExpense    = monthExpense,
            selectedPeriod  = period,
            searchQuery     = query,
            monthlyGoal     = goal,
            bankBalances    = bankBalances
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState()
    )

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSearchQueryChanged -> _searchQuery.value = event.query
            is DashboardEvent.OnPeriodChanged      -> _selectedPeriod.value = event.period
            is DashboardEvent.DeleteTransaction    -> {
                viewModelScope.launch {
                    transactionUseCases.deleteTransaction(event.transaction)
                }
            }
            is DashboardEvent.DeleteAllTransactions -> {
                viewModelScope.launch {
                    transactionUseCases.deleteAllTransactions()
                }
            }
        }
    }
}
