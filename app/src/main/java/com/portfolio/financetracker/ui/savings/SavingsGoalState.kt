package com.portfolio.financetracker.ui.savings

import com.portfolio.financetracker.domain.model.SavingsGoal

data class SavingsGoalState(
    val goals: List<SavingsGoal> = emptyList(),
    val totalSavings: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0
)
