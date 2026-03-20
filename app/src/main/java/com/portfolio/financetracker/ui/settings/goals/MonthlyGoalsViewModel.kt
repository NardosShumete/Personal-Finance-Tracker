package com.portfolio.financetracker.ui.settings.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.MonthlyGoal
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MonthlyGoalsViewModel @Inject constructor(
    private val goalUseCases: GoalUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyGoalsState())
    val state: StateFlow<MonthlyGoalsState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadCurrentMonthGoal()
    }

    private fun getCurrentMonthYear(): String {
        return SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
    }

    private fun loadCurrentMonthGoal() {
        val currentMonthYear = getCurrentMonthYear()
        _state.value = state.value.copy(monthYear = currentMonthYear, isLoading = true)

        viewModelScope.launch {
            goalUseCases.getGoal(currentMonthYear).collect { goal ->
                if (goal != null) {
                    _state.value = state.value.copy(
                        incomeGoal = if (goal.incomeGoal > 0) goal.incomeGoal.toString() else "",
                        expenseLimit = if (goal.expenseLimit > 0) goal.expenseLimit.toString() else "",
                        isLoading = false
                    )
                } else {
                    _state.value = state.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onEvent(event: MonthlyGoalsEvent) {
        when (event) {
            is MonthlyGoalsEvent.EnteredIncomeGoal -> {
                _state.value = state.value.copy(incomeGoal = event.value)
            }
            is MonthlyGoalsEvent.EnteredExpenseLimit -> {
                _state.value = state.value.copy(expenseLimit = event.value)
            }
            is MonthlyGoalsEvent.SaveGoals -> {
                viewModelScope.launch {
                    try {
                        goalUseCases.saveGoal(
                            MonthlyGoal(
                                monthYear = state.value.monthYear,
                                incomeGoal = state.value.incomeGoal.toDoubleOrNull() ?: 0.0,
                                expenseLimit = state.value.expenseLimit.toDoubleOrNull() ?: 0.0
                            )
                        )
                        _eventFlow.emit(UiEvent.SaveSuccess)
                    } catch (e: Exception) {
                        _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Couldn't save goals"))
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }
}
