package com.portfolio.financetracker.ui.settings.goals

import com.portfolio.financetracker.domain.model.CategoryBudget

data class MonthlyGoalsState(
    val monthYear: String = "",
    val incomeGoal: String = "",
    val expenseLimit: String = "",
    val isBudgetAlertsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val totalSpent: Double = 0.0,
    val highestSpendingCategory: String = "None",
    val insightMessage: String = ""
) {
    val remainingBudget: Double
        get() = (expenseLimit.toDoubleOrNull() ?: 0.0) - totalSpent
}
