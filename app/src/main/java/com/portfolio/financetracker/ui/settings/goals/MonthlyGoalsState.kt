package com.portfolio.financetracker.ui.settings.goals

data class MonthlyGoalsState(
    val monthYear: String = "",
    val incomeGoal: String = "",
    val expenseLimit: String = "",
    val isLoading: Boolean = false
)
