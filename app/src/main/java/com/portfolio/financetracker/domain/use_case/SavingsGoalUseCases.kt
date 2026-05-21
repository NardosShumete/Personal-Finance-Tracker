package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavingsGoals @Inject constructor(private val repository: SavingsGoalRepository) {
    operator fun invoke(): Flow<List<SavingsGoal>> = repository.getAllGoals()
}

class GetSavingsGoalById @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(id: Int): SavingsGoal? = repository.getGoalById(id)
}

class AddSavingsGoal @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.insertGoal(goal)
}

class UpdateSavingsGoal @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.updateGoal(goal)
}

class DeleteSavingsGoal @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.deleteGoal(goal)
}

class AddMoneyToGoal @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(id: Int, amount: Double) = repository.addMoney(id, amount)
}

class WithdrawMoneyFromGoal @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(id: Int, amount: Double) = repository.withdrawMoney(id, amount)
}

class UpdateGoalStatus @Inject constructor(private val repository: SavingsGoalRepository) {
    suspend operator fun invoke(id: Int, status: SavingsGoalStatus) = repository.updateStatus(id, status)
}

class GetTotalSavings @Inject constructor(private val repository: SavingsGoalRepository) {
    operator fun invoke(): Flow<Double> = repository.getTotalSavings()
}

data class SavingsGoalUseCases(
    val getSavingsGoals: GetSavingsGoals,
    val getSavingsGoalById: GetSavingsGoalById,
    val addSavingsGoal: AddSavingsGoal,
    val updateSavingsGoal: UpdateSavingsGoal,
    val deleteSavingsGoal: DeleteSavingsGoal,
    val addMoneyToGoal: AddMoneyToGoal,
    val withdrawMoneyFromGoal: WithdrawMoneyFromGoal,
    val updateGoalStatus: UpdateGoalStatus,
    val getTotalSavings: GetTotalSavings
)
