package com.portfolio.financetracker.ui.charts

data class ChartsState(
    val categoryExpenses: Map<String, Double> = emptyMap(),
    val totalExpense: Double = 0.0
)
