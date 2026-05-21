package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.CategoryBudgetEntity
import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import com.portfolio.financetracker.domain.model.CategoryBudget
import com.portfolio.financetracker.domain.model.MonthlyGoal

fun MonthlyGoalEntity.toDomainModel(): MonthlyGoal = MonthlyGoal(
    monthYear = monthYear,
    incomeGoal = incomeGoal,
    expenseLimit = expenseLimit
)

fun MonthlyGoal.toEntityModel(): MonthlyGoalEntity = MonthlyGoalEntity(
    monthYear = monthYear,
    incomeGoal = incomeGoal,
    expenseLimit = expenseLimit
)

fun CategoryBudgetEntity.toDomainModel(spentAmount: Double = 0.0): CategoryBudget = CategoryBudget(
    id = id,
    monthYear = monthYear,
    category = category,
    limitAmount = limitAmount,
    spentAmount = spentAmount
)

fun CategoryBudget.toEntityModel(): CategoryBudgetEntity = CategoryBudgetEntity(
    id = id,
    monthYear = monthYear,
    category = category,
    limitAmount = limitAmount
)
