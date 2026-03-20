package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.domain.model.MonthlyGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalByMonth(monthYear: String): Flow<MonthlyGoal?>
    suspend fun saveGoal(goal: MonthlyGoal)
}
