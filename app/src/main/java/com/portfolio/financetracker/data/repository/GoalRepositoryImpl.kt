package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.CategoryBudgetDao
import com.portfolio.financetracker.data.local.dao.MonthlyGoalDao
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.data.mapper.toEntityModel
import com.portfolio.financetracker.domain.model.CategoryBudget
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val goalDao: MonthlyGoalDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val transactionDao: TransactionDao
) : GoalRepository {
    override fun getGoalByMonth(monthYear: String): Flow<MonthlyGoal?> {
        return goalDao.getGoalByMonth(monthYear).map { it?.toDomainModel() }
    }

    override suspend fun saveGoal(goal: MonthlyGoal) {
        goalDao.insertGoal(goal.toEntityModel())
    }

    override fun getCategoryBudgets(monthYear: String): Flow<List<CategoryBudget>> {
        // We need to combine category limits with actual spending from transactions
        return combine(
            categoryBudgetDao.getCategoryBudgets(monthYear),
            transactionDao.getAllTransactions()
        ) { budgets, allTransactions ->
            // Filter transactions for the current month (parsed from monthYear)
            // monthYear is "MM-yyyy"
            val parts = monthYear.split("-")
            val month = parts[0].toInt() - 1 // Calendar months are 0-based
            val year = parts[1].toInt()

            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(java.util.Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis

            val monthExpenses = allTransactions.filter { 
                it.date in startOfMonth until endOfMonth && it.type == "EXPENSE"
            }

            budgets.map { entity ->
                val spent = monthExpenses
                    .filter { it.category.equals(entity.category, ignoreCase = true) }
                    .sumOf { it.amount }
                entity.toDomainModel(spent)
            }
        }
    }

    override suspend fun saveCategoryBudget(budget: CategoryBudget) {
        categoryBudgetDao.insertCategoryBudget(budget.toEntityModel())
    }

    override suspend fun clearBudgetsForMonth(monthYear: String) {
        categoryBudgetDao.clearBudgetsForMonth(monthYear)
    }
}
