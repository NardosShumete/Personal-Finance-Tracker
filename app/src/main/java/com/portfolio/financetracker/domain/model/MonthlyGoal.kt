package com.portfolio.financetracker.domain.model

data class MonthlyGoal(
    val monthYear: String, // Format: "MM-yyyy"
    val incomeGoal: Double,
    val expenseLimit: Double
)
