package com.portfolio.financetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
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
import kotlinx.coroutines.flow.update
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
    private val goalUseCases: GoalUseCases
) : ViewModel() {

    private val _searchQuery   = MutableStateFlow("")
    private val _selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)

    val pagedTransactions: Flow<PagingData<Transaction>> =
        transactionUseCases.getPagedTransactions().cachedIn(viewModelScope)

    val state: StateFlow<DashboardState> = combine(
        transactionUseCases.getTransactions(),
        _searchQuery,
        _selectedPeriod,
        goalUseCases.getGoal(
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        )
    ) { allTransactions, query, period, goal ->

        // Only confirmed transactions affect any totals
        val confirmed = allTransactions.filter { !it.isPending }

        // ── Time boundaries ───────────────────────────────────────────────────
        val now = Calendar.getInstance()

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

        // ── Search filter (list display only — never affects totals) ──────────
        val displayList = if (query.isBlank()) confirmed
        else confirmed.filter {
            it.category.contains(query, ignoreCase = true) ||
            it.note.contains(query, ignoreCase = true)
        }

        // ── Per-bank balances ─────────────────────────────────────────────────
        val bankBalances = confirmed
            .filter { it.bankName != null }
            .groupBy { it.bankName!! }
            .mapValues { (bankName, txns) ->
                val bIncome  = txns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
                val bExpense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                BankBalance(name = bankName, balance = bIncome - bExpense, income = bIncome, expense = bExpense)
            }

        DashboardState(
            totalBalance    = totalIncome - totalExpense,
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
        }
    }
}
