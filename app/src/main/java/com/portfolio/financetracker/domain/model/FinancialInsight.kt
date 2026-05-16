package com.portfolio.financetracker.domain.model

data class FinancialInsight(
    val title: String,
    val message: String,
    val type: InsightType,
    val priority: InsightPriority
)

enum class InsightType {
    SAVINGS, SPENDING, BUDGET, PREDICTION, TREND
}

enum class InsightPriority {
    INFO, WARNING, SUCCESS
}

data class InsightsData(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val dailyAverage: Double,
    val predictedBurnRate: Double,
    val budgetUsage: Float,
    val remainingBudget: Double,
    val categoryWiseExpenses: Map<String, Double>,
    val alerts: List<FinancialInsight>,
    val humanReadableInsights: List<FinancialInsight>,
    val weeklySpending: Map<Int, Double> // Week of month to amount
)
