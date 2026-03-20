package com.portfolio.financetracker.ui.settings.goals

sealed class MonthlyGoalsEvent {
    data class EnteredIncomeGoal(val value: String) : MonthlyGoalsEvent()
    data class EnteredExpenseLimit(val value: String) : MonthlyGoalsEvent()
    object SaveGoals : MonthlyGoalsEvent()
}
