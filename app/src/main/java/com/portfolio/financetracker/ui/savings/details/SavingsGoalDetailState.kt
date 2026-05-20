package com.portfolio.financetracker.ui.savings.details

import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalTransaction

data class SavingsGoalDetailState(
    val goal: SavingsGoal? = null,
    val transactions: List<SavingsGoalTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
