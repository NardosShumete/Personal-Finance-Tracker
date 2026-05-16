package com.portfolio.financetracker.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.InsightsData
import com.portfolio.financetracker.domain.use_case.GetInsightsUseCase
import com.portfolio.financetracker.domain.use_case.GetTransactionsUseCase
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val goalUseCases: GoalUseCases,
    private val getInsightsUseCase: GetInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy"))
        
        combine(
            getTransactionsUseCase(),
            goalUseCases.getGoal(currentMonthYear)
        ) { transactions, goal ->
            val insightsData = getInsightsUseCase(transactions, goal)
            InsightsUiState(
                insightsData = insightsData,
                isLoading = false
            )
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.onEach { newState ->
            _uiState.value = newState
        }.catch { e ->
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(viewModelScope)
    }
}

data class InsightsUiState(
    val insightsData: InsightsData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
