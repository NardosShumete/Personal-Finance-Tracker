package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.SavingsGoalEntity
import com.portfolio.financetracker.domain.model.SavingsGoal

fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = id,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = startDate,
        deadlineDate = deadlineDate,
        category = category,
        colorHex = colorHex,
        iconResId = iconResId,
        status = status,
        isPinned = isPinned
    )
}

fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = startDate,
        deadlineDate = deadlineDate,
        category = category,
        colorHex = colorHex,
        iconResId = iconResId,
        status = status,
        isPinned = isPinned
    )
}
