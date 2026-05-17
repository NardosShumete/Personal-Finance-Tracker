package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.BuildConfig
import com.portfolio.financetracker.data.remote.groq.*
import com.portfolio.financetracker.domain.repository.AiRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val api: GroqApi
) : AiRepository {

    init {
        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            throw IllegalStateException("Groq API key missing. Please add GROQ_API_KEY to your local.properties file.")
        }
    }

    private val apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}"
    
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAiInsights(
        transactionsJson: String,
        budgetsJson: String
    ): AiInsightResponse {
        val prompt = """
            You are a smart financial assistant. Analyze the following financial data and provide insights.
            
            Transactions:
            $transactionsJson
            
            Budgets:
            $budgetsJson
            
            Provide the response in the following JSON format:
            {
                "summary": "Overall financial health summary",
                "daily_avg_insight": "Insight about daily average spending",
                "budget_warning": "Warning if near or over budget, else null",
                "unusual_spending": ["List of unusual transactions or patterns"],
                "recommendations": ["Actionable financial advice"],
                "predicted_burn_rate_message": "Prediction about end-of-month spending"
            }
            
            Be concise, human-friendly, and avoid technical jargon. Use ETB as currency.
        """.trimIndent()

        val request = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage(role = "system", content = "You are a helpful financial advisor."),
                GroqMessage(role = "user", content = prompt)
            ),
            response_format = GroqResponseFormat(type = "json_object")
        )

        val response = api.getChatCompletion(apiKey, request)
        var content = response.choices.firstOrNull()?.message?.content ?: throw Exception("Empty AI response")
        
        // Clean markdown if present
        if (content.contains("```json")) {
            content = content.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (content.contains("```")) {
            content = content.substringAfter("```").substringBeforeLast("```").trim()
        }
        
        return json.decodeFromString<AiInsightResponse>(content)
    }

    override suspend fun getChatResponse(messages: List<GroqMessage>): String {
        val request = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = messages
        )

        val response = api.getChatCompletion(apiKey, request)
        return response.choices.firstOrNull()?.message?.content ?: throw Exception("Empty AI response")
    }
}
