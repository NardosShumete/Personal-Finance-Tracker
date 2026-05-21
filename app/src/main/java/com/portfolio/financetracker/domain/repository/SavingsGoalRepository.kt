package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getAllGoals(): Flow<List<SavingsGoal>>
    suspend fun getGoalById(id: Int): SavingsGoal?
    suspend fun insertGoal(goal: SavingsGoal)
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goal: SavingsGoal)
    suspend fun addMoney(id: Int, amount: Double)
    suspend fun withdrawMoney(id: Int, amount: Double)
    suspend fun updateStatus(id: Int, status: SavingsGoalStatus)
    fun getTotalSavings(): Flow<Double>
}
