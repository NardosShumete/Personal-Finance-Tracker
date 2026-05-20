package com.portfolio.financetracker.ui.settings.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.CategoryBudget
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
    private val goalUseCases: GoalUseCases,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyGoalsState())
    val state: StateFlow<MonthlyGoalsState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val defaultCategories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Savings")

    init {
        val currentMonthYear = getCurrentMonthYear()
        _state.update { it.copy(monthYear = currentMonthYear) }
        loadData()
    }

    private fun getCurrentMonthYear(): String {
        return SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
    }

    private fun loadData() {
        val currentMonthYear = state.value.monthYear
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // Load Main Goals
            val goalFlow = goalUseCases.getGoal(currentMonthYear)
            // Load Category Budgets
            val categoryBudgetsFlow = goalUseCases.getCategoryBudgets(currentMonthYear)
            // Load Settings
            val alertsEnabledFlow = dataStoreManager.isBudgetAlertsEnabled

            combine(goalFlow, categoryBudgetsFlow, alertsEnabledFlow) { goal, budgets, alertsEnabled ->
                // If no budgets exist, initialize with defaults
                val finalBudgets = if (budgets.isEmpty()) {
                    defaultCategories.map { CategoryBudget(monthYear = currentMonthYear, category = it, limitAmount = 0.0) }
                } else {
                    budgets
                }

                val totalSpent = finalBudgets.sumOf { it.spentAmount }
                val highestCategory = finalBudgets.maxByOrNull { it.spentAmount }?.category ?: "None"
                
                val insight = if (totalSpent > (goal?.expenseLimit ?: 0.0) && (goal?.expenseLimit ?: 0.0) > 0) {
                    "You have exceeded your total budget! Try reducing $highestCategory expenses."
                } else if (highestCategory != "None") {
                    "Your highest spending is in $highestCategory. Keep an eye on it."
                } else {
                    "Set your limits to start tracking your budget status."
                }

                _state.update { it.copy(
                    incomeGoal = if (goal != null && goal.incomeGoal > 0) goal.incomeGoal.toString() else it.incomeGoal,
                    expenseLimit = if (goal != null && goal.expenseLimit > 0) goal.expenseLimit.toString() else it.expenseLimit,
                    categoryBudgets = finalBudgets,
                    isBudgetAlertsEnabled = alertsEnabled,
                    totalSpent = totalSpent,
                    highestSpendingCategory = highestCategory,
                    insightMessage = insight,
                    isLoading = false
                ) }
            }.collect()
        }
    }

    fun onEvent(event: MonthlyGoalsEvent) {
        when (event) {
            is MonthlyGoalsEvent.EnteredIncomeGoal -> {
                _state.update { it.copy(incomeGoal = event.value) }
            }
            is MonthlyGoalsEvent.EnteredExpenseLimit -> {
                _state.update { it.copy(expenseLimit = event.value) }
            }
            is MonthlyGoalsEvent.ToggleBudgetAlerts -> {
                viewModelScope.launch {
                    dataStoreManager.setBudgetAlertsEnabled(event.enabled)
                }
            }
            is MonthlyGoalsEvent.EnteredCategoryLimit -> {
                val updatedBudgets = state.value.categoryBudgets.map {
                    if (it.category == event.category) {
                        it.copy(limitAmount = event.limit.toDoubleOrNull() ?: 0.0)
                    } else it
                }
                _state.update { it.copy(categoryBudgets = updatedBudgets) }
            }
            is MonthlyGoalsEvent.SaveGoals -> {
                viewModelScope.launch {
                    try {
                        // Save Main Goal
                        goalUseCases.saveGoal(
                            MonthlyGoal(
                                monthYear = state.value.monthYear,
                                incomeGoal = state.value.incomeGoal.toDoubleOrNull() ?: 0.0,
                                expenseLimit = state.value.expenseLimit.toDoubleOrNull() ?: 0.0
                            )
                        )
                        // Save Category Budgets
                        state.value.categoryBudgets.forEach {
                            goalUseCases.saveCategoryBudget(it)
                        }
                        _eventFlow.emit(UiEvent.SaveSuccess)
                    } catch (e: Exception) {
                        _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Couldn't save goals"))
                    }
                }
            }
            is MonthlyGoalsEvent.ResetBudgets -> {
                viewModelScope.launch {
                    goalUseCases.clearBudgetsForMonth(state.value.monthYear)
                    _state.update { it.copy(
                        incomeGoal = "",
                        expenseLimit = "",
                        categoryBudgets = defaultCategories.map { cat -> 
                            CategoryBudget(monthYear = state.value.monthYear, category = cat, limitAmount = 0.0) 
                        }
                    ) }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }
}
