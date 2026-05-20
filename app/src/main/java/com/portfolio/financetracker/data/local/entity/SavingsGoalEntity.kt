package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.portfolio.financetracker.domain.model.SavingsGoalStatus

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
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
    val isPinned: Boolean = false,
    val imagePath: String? = null,
    val lastContributionDate: Long? = null,
    val currentStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
