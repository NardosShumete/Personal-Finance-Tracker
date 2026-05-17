package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.data.remote.groq.GroqMessage
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.AiTransaction
import com.portfolio.financetracker.domain.repository.AiRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GetAiChatResponseUseCase @Inject constructor(
    private val repository: AiRepository
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(
        userMessage: String,
        chatHistory: List<GroqMessage>,
        transactions: List<Transaction>,
        currentGoal: MonthlyGoal?
    ): String {
        val aiTransactions = transactions.map { 
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

        val systemPrompt = """
            You are a smart financial assistant for the "Personal Finance Tracker" app.
            You have access to the user's financial data below.
            
            Transactions:
            $transactionsJson
            
            Budgets:
            $budgetsJson
            
            Help the user by answering their questions about their financial situation, spending patterns, and giving advice.
            Be concise, human-friendly, and avoid technical jargon. Use ETB as currency.
            If they ask about something not in the data, try to be helpful based on general financial principles.
        """.trimIndent()

        val messages = mutableListOf<GroqMessage>()
        messages.add(GroqMessage(role = "system", content = systemPrompt))
        messages.addAll(chatHistory)
        messages.add(GroqMessage(role = "user", content = userMessage))

        return repository.getChatResponse(messages)
    }
}
