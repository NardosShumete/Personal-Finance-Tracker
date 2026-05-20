package com.portfolio.financetracker.ui.settings.goals

sealed class MonthlyGoalsEvent {
    data class EnteredIncomeGoal(val value: String) : MonthlyGoalsEvent()
    data class EnteredExpenseLimit(val value: String) : MonthlyGoalsEvent()
    data class ToggleBudgetAlerts(val enabled: Boolean) : MonthlyGoalsEvent()
    data class EnteredCategoryLimit(val category: String, val limit: String) : MonthlyGoalsEvent()
    object SaveGoals : MonthlyGoalsEvent()
    object ResetBudgets : MonthlyGoalsEvent()
}
