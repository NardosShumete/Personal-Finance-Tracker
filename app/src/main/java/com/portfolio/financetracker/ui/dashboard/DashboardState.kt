package com.portfolio.financetracker.ui.dashboard

import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.Transaction

enum class SummaryPeriod { TODAY, THIS_MONTH, ALL_TIME }

data class DashboardState(
    val transactions: List<Transaction> = emptyList(),

    // ── All-time totals (used for net balance) ────────────────────────────────
    val totalBalance: Double = 0.0,
    val totalIncome: Double  = 0.0,
    val totalExpense: Double = 0.0,

    // ── Period-specific totals ────────────────────────────────────────────────
    val todayIncome: Double   = 0.0,
    val todayExpense: Double  = 0.0,
    val monthIncome: Double   = 0.0,
    val monthExpense: Double  = 0.0,

    // ── Active period selection ───────────────────────────────────────────────
    val selectedPeriod: SummaryPeriod = SummaryPeriod.THIS_MONTH,

    val searchQuery: String = "",
    val isLoading: Boolean  = false,
    val monthlyGoal: MonthlyGoal? = null,
    val bankBalances: Map<String, BankBalance> = emptyMap()
) {
    /** Income shown in the summary card based on the selected period. */
    val displayIncome: Double get() = when (selectedPeriod) {
        SummaryPeriod.TODAY      -> todayIncome
        SummaryPeriod.THIS_MONTH -> monthIncome
        SummaryPeriod.ALL_TIME   -> totalIncome
    }

    /** Expense shown in the summary card based on the selected period. */
    val displayExpense: Double get() = when (selectedPeriod) {
        SummaryPeriod.TODAY      -> todayExpense
        SummaryPeriod.THIS_MONTH -> monthExpense
        SummaryPeriod.ALL_TIME   -> totalExpense
    }
}

data class BankBalance(
    val name: String,
    val balance: Double,
    val income: Double,
    val expense: Double
)
