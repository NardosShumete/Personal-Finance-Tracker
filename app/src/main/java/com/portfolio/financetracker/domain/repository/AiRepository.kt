package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.data.remote.groq.AiInsightResponse
import com.portfolio.financetracker.data.remote.groq.GroqMessage

interface AiRepository {
    suspend fun getAiInsights(
        transactionsJson: String,
        budgetsJson: String
    ): AiInsightResponse

    suspend fun getChatResponse(
        messages: List<GroqMessage>
    ): String
}
