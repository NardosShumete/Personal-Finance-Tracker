package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.SavingsGoalDao
import com.portfolio.financetracker.data.local.dao.SavingsGoalTransactionDao
import com.portfolio.financetracker.data.local.entity.SavingsGoalTransactionEntity
import com.portfolio.financetracker.data.mapper.toDomain
import com.portfolio.financetracker.data.mapper.toEntity
import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsGoalRepositoryImpl(
    private val dao: SavingsGoalDao,
    private val transactionDao: SavingsGoalTransactionDao
) : SavingsGoalRepository {
    override fun getAllGoals(): Flow<List<SavingsGoal>> {
        return dao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getGoalById(id: Int): SavingsGoal? {
        return dao.getGoalById(id)?.toDomain()
    }

    override suspend fun insertGoal(goal: SavingsGoal) {
        dao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        dao.updateGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        dao.deleteGoal(goal.toEntity())
    }

    override suspend fun addMoney(id: Int, amount: Double) {
        val goal = dao.getGoalById(id) ?: return
        val newAmount = goal.currentAmount + amount
        dao.updateCurrentAmount(id, newAmount)
        
        transactionDao.insertTransaction(
            SavingsGoalTransactionEntity(
                goalId = id,
                amount = amount,
                type = "DEPOSIT"
            )
        )
        
        // Auto-complete if target reached
        if (newAmount >= goal.targetAmount) {
            dao.updateStatus(id, SavingsGoalStatus.COMPLETED)
        }
    }

    override suspend fun withdrawMoney(id: Int, amount: Double) {
        val goal = dao.getGoalById(id) ?: return
        val newAmount = (goal.currentAmount - amount).coerceAtLeast(0.0)
        dao.updateCurrentAmount(id, newAmount)

        transactionDao.insertTransaction(
            SavingsGoalTransactionEntity(
                goalId = id,
                amount = amount,
                type = "WITHDRAWAL"
            )
        )
        
        // Revert to active if withdrawn below target
        if (newAmount < goal.targetAmount && goal.status == SavingsGoalStatus.COMPLETED) {
            dao.updateStatus(id, SavingsGoalStatus.ACTIVE)
        }
    }

    override suspend fun updateStatus(id: Int, status: SavingsGoalStatus) {
        dao.updateStatus(id, status)
    }

    override fun getTotalSavings(): Flow<Double> {
        return dao.getTotalSavings().map { it ?: 0.0 }
    }
}
