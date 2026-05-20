package com.portfolio.financetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryBudget(
    val id: Int = 0,
    val monthYear: String,
    val category: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0
) {
    val remainingAmount: Double
        get() = limitAmount - spentAmount
    
    val progress: Float
        get() = if (limitAmount > 0) (spentAmount / limitAmount).toFloat() else 0f
}
