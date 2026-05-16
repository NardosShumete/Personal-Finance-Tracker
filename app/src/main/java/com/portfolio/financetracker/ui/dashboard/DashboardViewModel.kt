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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val goalUseCases: GoalUseCases
) : ViewModel() {

    // ── Search query ──────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")

    // ── Paged transaction list ────────────────────────────────────────────────
    // cachedIn(viewModelScope) keeps the paged data alive across recompositions
    // and survives configuration changes without re-querying the DB.
    val pagedTransactions: Flow<PagingData<Transaction>> =
        transactionUseCases.getPagedTransactions()
            .cachedIn(viewModelScope)

    // ── Summary state (balance / income / expense / goal) ────────────────────
    // Uses stateIn() so the Flow is only collected once and the latest value
    // is replayed instantly to new subscribers (e.g. after recomposition).
    // SharingStarted.WhileSubscribed(5_000) keeps the upstream alive for 5 s
    // after the last subscriber drops — survives brief config changes cheaply.
    val state: StateFlow<DashboardState> = combine(
        // Full list needed for accurate totals (paged data is a subset)
        transactionUseCases.getTransactions(),
        _searchQuery,
        goalUseCases.getGoal(
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        )
    ) { allTransactions, query, goal ->
        val filtered = if (query.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter {
                it.category.contains(query, ignoreCase = true) ||
                it.note.contains(query, ignoreCase = true)
            }
        }

        val income  = filtered.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val bankBalances = allTransactions
            .filter { it.bankName != null }
            .groupBy { it.bankName!! }
            .mapValues { (bankName, transactions) ->
                val bankIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val bankExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                BankBalance(
                    name = bankName,
                    balance = bankIncome - bankExpense,
                    income = bankIncome,
                    expense = bankExpense
                )
            }

        DashboardState(
            totalBalance = income - expense,
            totalIncome  = income,
            totalExpense = expense,
            searchQuery  = query,
            monthlyGoal  = goal,
            bankBalances = bankBalances
        )
    }.stateIn(
        scope            = viewModelScope,
        started          = SharingStarted.WhileSubscribed(5_000),
        initialValue     = DashboardState()
    )

    // ── Events ────────────────────────────────────────────────────────────────
    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSearchQueryChanged -> {
                _searchQuery.value = event.query
            }
            is DashboardEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    transactionUseCases.deleteTransaction(event.transaction)
                    // Room invalidates the PagingSource automatically after delete
                }
            }
        }
    }
}
