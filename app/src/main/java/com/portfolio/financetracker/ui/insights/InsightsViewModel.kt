package com.portfolio.financetracker.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.remote.groq.AiInsightResponse
import com.portfolio.financetracker.data.remote.groq.GroqMessage
import com.portfolio.financetracker.domain.model.InsightsData
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.use_case.GetAiChatResponseUseCase
import com.portfolio.financetracker.domain.use_case.GetAiInsightsUseCase
import com.portfolio.financetracker.domain.use_case.GetInsightsUseCase
import com.portfolio.financetracker.domain.use_case.GetTransactionsUseCase
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val goalUseCases: GoalUseCases,
    private val getInsightsUseCase: GetInsightsUseCase,
    private val getAiInsightsUseCase: GetAiInsightsUseCase,
    private val getAiChatResponseUseCase: GetAiChatResponseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var currentGoal: com.portfolio.financetracker.domain.model.MonthlyGoal? = null

    init {
        loadInsights()
    }

    private fun loadInsights() {
        val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy"))
        
        combine(
            getTransactionsUseCase(),
            goalUseCases.getGoal(currentMonthYear)
        ) { transactions, goal ->
            allTransactions = transactions
            currentGoal = goal
            
            val insightsData = getInsightsUseCase(transactions, goal)
            
            _uiState.update { 
                it.copy(
                    insightsData = insightsData,
                    isLoading = false
                )
            }
            
            // Trigger AI insights if they aren't loaded yet
            if (_uiState.value.aiInsights == null && !_uiState.value.isAiLoading) {
                getAiInsights(transactions, goal)
            }
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.catch { e ->
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(viewModelScope)
    }

    private fun getAiInsights(transactions: List<Transaction>, goal: com.portfolio.financetracker.domain.model.MonthlyGoal?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            try {
                val aiInsights = getAiInsightsUseCase(transactions, goal)
                _uiState.update { it.copy(aiInsights = aiInsights, isAiLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAiLoading = false, aiError = e.message) }
            }
        }
    }

    fun onSendMessage(message: String) {
        if (message.isBlank()) return

        val userMsg = GroqMessage(role = "user", content = message)
        _uiState.update { 
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val response = getAiChatResponseUseCase(
                    userMessage = message,
                    chatHistory = _uiState.value.chatMessages.dropLast(1),
                    transactions = allTransactions,
                    currentGoal = currentGoal
                )
                val assistantMsg = GroqMessage(role = "assistant", content = response)
                _uiState.update { 
                    it.copy(
                        chatMessages = it.chatMessages + assistantMsg,
                        isChatLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isChatLoading = false, chatError = e.message) }
            }
        }
    }

    fun clearChatError() {
        _uiState.update { it.copy(chatError = null) }
    }
}

data class InsightsUiState(
    val insightsData: InsightsData? = null,
    val aiInsights: AiInsightResponse? = null,
    val chatMessages: List<GroqMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isAiLoading: Boolean = false,
    val isChatLoading: Boolean = false,
    val error: String? = null,
    val aiError: String? = null,
    val chatError: String? = null
)
