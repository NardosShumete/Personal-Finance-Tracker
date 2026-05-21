package com.portfolio.financetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import com.portfolio.financetracker.domain.use_case.SavingsGoalUseCases
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val goalUseCases: GoalUseCases,
    private val savingsGoalUseCases: SavingsGoalUseCases
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val pagedTransactions: Flow<PagingData<Transaction>> =
        transactionUseCases.getPagedTransactions()
            .cachedIn(viewModelScope)

    val state: StateFlow<DashboardState> = combine(
        transactionUseCases.getTransactions(),
        _searchQuery,
        goalUseCases.getGoal(SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())),
        savingsGoalUseCases.getTotalSavings(),
        savingsGoalUseCases.getSavingsGoals()
    ) { allTransactions, query, goal, totalSavings, savingsGoals ->
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
            bankBalances = bankBalances,
            totalSavings = totalSavings,
            activeSavingsGoalsCount = savingsGoals.count { it.status == SavingsGoalStatus.ACTIVE }
        )
    }.stateIn(
        scope            = viewModelScope,
        started          = SharingStarted.WhileSubscribed(5_000),
        initialValue     = DashboardState()
    )

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSearchQueryChanged -> {
                _searchQuery.value = event.query
            }
            is DashboardEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    transactionUseCases.deleteTransaction(event.transaction)
                }
            }
            is DashboardEvent.SaveGoal -> {
                viewModelScope.launch {
                    val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
                    goalUseCases.saveGoal(
                        MonthlyGoal(
                            monthYear = currentMonthYear,
                            incomeGoal = event.incomeGoal,
                            expenseLimit = event.expenseLimit
                        )
                    )
                }
            }
        }
    }
}
