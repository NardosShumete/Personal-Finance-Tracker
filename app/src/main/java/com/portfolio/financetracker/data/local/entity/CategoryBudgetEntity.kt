package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budget_table")
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monthYear: String,
    val category: String,
    val limitAmount: Double
)
