package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.model.*
import java.time.*
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class GetInsightsUseCase @Inject constructor() {
    operator fun invoke(
        transactions: List<Transaction>,
        currentGoal: MonthlyGoal?
    ): InsightsData {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        // Filter for current month
        val currentMonthTransactions = transactions.filter {
            val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            date.monthValue == currentMonth && date.year == currentYear && !it.isPending
        }

        val totalIncome = currentMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses
        
        val daysInMonth = now.lengthOfMonth()
        val currentDay = now.dayOfMonth
        val dailyAverage = if (currentDay > 0) totalExpenses / currentDay else 0.0
        val predictedBurnRate = dailyAverage * daysInMonth
        
        val expenseLimit = currentGoal?.expenseLimit ?: 0.0
        val budgetUsage = if (expenseLimit > 0) (totalExpenses / expenseLimit).toFloat().coerceIn(0f, 1.2f) else 0f
        val remainingBudget = (expenseLimit - totalExpenses).coerceAtLeast(0.0)

        val categoryWiseExpenses = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }

        val weeklySpending = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { 
                val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                ((date.dayOfMonth - 1) / 7) + 1
            }
            .mapValues { it.value.sumOf { t -> t.amount } }

        val alerts = mutableListOf<FinancialInsight>()
        val insights = mutableListOf<FinancialInsight>()

        // 1. Budget Alert
        if (expenseLimit > 0) {
            if (totalExpenses > expenseLimit) {
                alerts.add(FinancialInsight(
                    "Budget Exceeded",
                    "You've spent ${String.format("%.2f", totalExpenses - expenseLimit)} more than your limit of ${String.format("%.2f", expenseLimit)}.",
                    InsightType.BUDGET,
                    InsightPriority.WARNING
                ))
            } else if (budgetUsage > 0.8) {
                alerts.add(FinancialInsight(
                    "Budget Warning",
                    "You've used ${String.format("%.0f%%", budgetUsage * 100)} of your monthly budget.",
                    InsightType.BUDGET,
                    InsightPriority.INFO
                ))
            }
        }

        // 2. Savings Insight
        if (netSavings > 0) {
            insights.add(FinancialInsight(
                "Good Progress!",
                "You've saved ${String.format("%.2f", netSavings)} so far this month.",
                InsightType.SAVINGS,
                InsightPriority.SUCCESS
            ))
        } else if (totalExpenses > totalIncome && totalIncome > 0) {
            insights.add(FinancialInsight(
                "Spending high",
                "Your expenses are higher than your income this month. Consider cutting down on non-essentials.",
                InsightType.SAVINGS,
                InsightPriority.WARNING
            ))
        }

        // 3. Category Analysis
        val topCategory = categoryWiseExpenses.maxByOrNull { it.value }
        if (topCategory != null) {
            insights.add(FinancialInsight(
                "Top Spending",
                "You spent the most on ${topCategory.key} (${String.format("%.2f", topCategory.value)}).",
                InsightType.SPENDING,
                InsightPriority.INFO
            ))
        }

        // 4. Burn Rate Prediction
        if (predictedBurnRate > expenseLimit && expenseLimit > 0) {
            alerts.add(FinancialInsight(
                "Prediction Alert",
                "At this rate, you'll spend ${String.format("%.2f", predictedBurnRate)} by the end of the month, exceeding your budget.",
                InsightType.PREDICTION,
                InsightPriority.WARNING
            ))
        }
        
        // 5. Trend Analysis
        val previousMonthDate = now.minusMonths(1)
        val prevMonth = previousMonthDate.monthValue
        val prevYear = previousMonthDate.year
        
        val prevMonthExpenses = transactions.filter {
            val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            date.monthValue == prevMonth && date.year == prevYear && it.type == TransactionType.EXPENSE && !it.isPending
        }.sumOf { it.amount }

        if (prevMonthExpenses > 0) {
            val diff = totalExpenses - prevMonthExpenses
            val percent = (abs(diff) / prevMonthExpenses) * 100
            if (diff > 0) {
                 insights.add(FinancialInsight(
                    "Spending Trend",
                    "You've spent ${String.format("%.0f%%", percent)} more than last month.",
                    InsightType.TREND,
                    InsightPriority.WARNING
                ))
            } else {
                 insights.add(FinancialInsight(
                    "Spending Trend",
                    "Great! You're spending ${String.format("%.0f%%", percent)} less than last month.",
                    InsightType.TREND,
                    InsightPriority.SUCCESS
                ))
            }
        }

        return InsightsData(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            dailyAverage = dailyAverage,
            predictedBurnRate = predictedBurnRate,
            budgetUsage = budgetUsage,
            remainingBudget = remainingBudget,
            categoryWiseExpenses = categoryWiseExpenses,
            alerts = alerts,
            humanReadableInsights = insights,
            weeklySpending = weeklySpending
        )
    }
}
