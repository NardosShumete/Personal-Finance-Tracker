package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.domain.model.CategoryBudget
import com.portfolio.financetracker.domain.model.MonthlyGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalByMonth(monthYear: String): Flow<MonthlyGoal?>
    suspend fun saveGoal(goal: MonthlyGoal)
    
    fun getCategoryBudgets(monthYear: String): Flow<List<CategoryBudget>>
    suspend fun saveCategoryBudget(budget: CategoryBudget)
    suspend fun clearBudgetsForMonth(monthYear: String)
}
