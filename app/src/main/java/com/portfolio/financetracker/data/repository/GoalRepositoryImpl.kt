package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.MonthlyGoalDao
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.data.mapper.toEntityModel
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val dao: MonthlyGoalDao
) : GoalRepository {
    override fun getGoalByMonth(monthYear: String): Flow<MonthlyGoal?> {
        return dao.getGoalByMonth(monthYear).map { it?.toDomainModel() }
    }

    override suspend fun saveGoal(goal: MonthlyGoal) {
        dao.insertGoal(goal.toEntityModel())
    }
}
