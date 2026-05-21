package com.portfolio.financetracker.domain.model

data class SavingsGoalMilestone(
    val id: Int = 0,
    val goalId: Int,
    val title: String,
    val targetAmount: Double,
    val isReached: Boolean = false,
    val reachedAt: Long? = null
)
