package com.portfolio.financetracker.data.remote.groq

import kotlinx.serialization.Serializable

@Serializable
data class GroqRequest(
    val model: String = "llama3-70b-8192",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048,
    val response_format: GroqResponseFormat? = null
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponseFormat(
    val type: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)

@Serializable
data class AiInsightResponse(
    val summary: String,
    val daily_avg_insight: String,
    val budget_warning: String? = null,
    val unusual_spending: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val predicted_burn_rate_message: String
)
