package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.data.remote.groq.AiInsightResponse
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.AiTransaction
import com.portfolio.financetracker.domain.repository.AiRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

class GetAiInsightsUseCase @Inject constructor(
    private val repository: AiRepository
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(
        transactions: List<Transaction>,
        currentGoal: MonthlyGoal?
    ): AiInsightResponse {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year

        // Filter for current month only and limit to most recent 50 to stay within token limits
        val filteredTransactions = transactions
            .filter {
                val date = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                date.monthValue == currentMonth && date.year == currentYear && !it.isPending
            }
            .sortedByDescending { it.date }
            .take(50)

        // Map to a serializable DTO
        val aiTransactions = filteredTransactions.map {
            AiTransaction(
                amount = it.amount,
                category = it.category,
                type = it.type.name,
                date = it.date,
                note = it.note
            )
        }
        
        val transactionsJson = json.encodeToString(aiTransactions)
        val budgetsJson = json.encodeToString(currentGoal)

        return repository.getAiInsights(transactionsJson, budgetsJson)
    }
}
