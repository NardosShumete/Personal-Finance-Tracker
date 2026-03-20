package com.portfolio.financetracker.ui.dashboard

import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.Transaction

data class DashboardState(
    val transactions: List<Transaction> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val monthlyGoal: MonthlyGoal? = null
)
