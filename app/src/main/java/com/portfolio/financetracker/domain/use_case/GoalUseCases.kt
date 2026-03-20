package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalUseCase(
    private val repository: GoalRepository
) {
    operator fun invoke(monthYear: String): Flow<MonthlyGoal?> {
        return repository.getGoalByMonth(monthYear)
    }
}

class SaveGoalUseCase(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: MonthlyGoal) {
        repository.saveGoal(goal)
    }
}

data class GoalUseCases(
    val getGoal: GetGoalUseCase,
    val saveGoal: SaveGoalUseCase
)
