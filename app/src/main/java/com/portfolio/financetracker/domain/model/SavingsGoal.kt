package com.portfolio.financetracker.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class SavingsGoal(
    val id: Int = 0,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val startDate: Long,
    val deadlineDate: Long,
    val category: String,
    val colorHex: Long,
    val iconResId: Int?,
    val status: SavingsGoalStatus = SavingsGoalStatus.ACTIVE,
    val isPinned: Boolean = false
) {
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val progress: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val isOverdue: Boolean
        get() = System.currentTimeMillis() > deadlineDate && status == SavingsGoalStatus.ACTIVE
}
