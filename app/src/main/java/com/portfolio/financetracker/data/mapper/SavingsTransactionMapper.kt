package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.SavingsGoalTransactionEntity
import com.portfolio.financetracker.domain.model.SavingsGoalTransaction

fun SavingsGoalTransactionEntity.toDomain(): SavingsGoalTransaction {
    return SavingsGoalTransaction(
        id = id,
        goalId = goalId,
        amount = amount,
        type = type,
        timestamp = timestamp,
        note = note
    )
}
