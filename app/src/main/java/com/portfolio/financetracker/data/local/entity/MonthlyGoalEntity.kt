package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_goal_table")
data class MonthlyGoalEntity(
    @PrimaryKey
    val monthYear: String, // E.g. "03-2026"
    val incomeGoal: Double,
    val expenseLimit: Double
)
