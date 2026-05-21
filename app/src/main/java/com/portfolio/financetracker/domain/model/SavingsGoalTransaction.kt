package com.portfolio.financetracker.domain.model

data class SavingsGoalTransaction(
    val id: Int = 0,
    val goalId: Int,
    val amount: Double,
    val type: String, // "DEPOSIT" or "WITHDRAWAL"
    val timestamp: Long,
    val note: String? = null
)
