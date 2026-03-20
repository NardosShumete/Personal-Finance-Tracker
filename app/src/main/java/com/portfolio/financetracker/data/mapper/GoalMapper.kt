package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.MonthlyGoalEntity
import com.portfolio.financetracker.domain.model.MonthlyGoal

fun MonthlyGoalEntity.toDomainModel(): MonthlyGoal {
    return MonthlyGoal(
        monthYear = monthYear,
        incomeGoal = incomeGoal,
        expenseLimit = expenseLimit
    )
}

fun MonthlyGoal.toEntityModel(): MonthlyGoalEntity {
    return MonthlyGoalEntity(
        monthYear = monthYear,
        incomeGoal = incomeGoal,
        expenseLimit = expenseLimit
    )
}
