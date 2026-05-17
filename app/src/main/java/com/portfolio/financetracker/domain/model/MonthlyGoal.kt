package com.portfolio.financetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyGoal(
    val monthYear: String, // Format: "MM-yyyy"
    val incomeGoal: Double,
    val expenseLimit: Double
)
