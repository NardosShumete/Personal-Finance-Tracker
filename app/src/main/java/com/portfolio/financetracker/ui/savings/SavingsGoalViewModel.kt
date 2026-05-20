package com.portfolio.financetracker.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.domain.use_case.SavingsGoalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val useCases: SavingsGoalUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(SavingsGoalState())
    val state: StateFlow<SavingsGoalState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SortType.DEADLINE)

    init {
        getGoals()
    }

    private fun getGoals() {
        combine(
            useCases.getSavingsGoals(),
            _searchQuery,
            _sortBy,
            useCases.getTotalSavings()
        ) { goals, query, sortType, totalSavings ->
            val filteredGoals = goals.filter { 
                it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) 
            }
            
            val sortedGoals = when (sortType) {
                SortType.DEADLINE -> filteredGoals.sortedBy { it.deadlineDate }
                SortType.AMOUNT -> filteredGoals.sortedByDescending { it.targetAmount }
                SortType.PROGRESS -> filteredGoals.sortedByDescending { it.progress }
            }
            
            _state.value.copy(
                goals = sortedGoals,
                totalSavings = totalSavings,
                searchQuery = query,
                activeGoalsCount = goals.count { it.status == SavingsGoalStatus.ACTIVE },
                completedGoalsCount = goals.count { it.status == SavingsGoalStatus.COMPLETED }
            )
        }.onStart {
            _state.update { it.copy(isLoading = true) }
        }.onEach { newState ->
            _state.update { newState.copy(isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SavingsGoalEvent) {
        when (event) {
            is SavingsGoalEvent.AddGoal -> {
                viewModelScope.launch {
                    useCases.addSavingsGoal(event.goal)
                }
            }
            is SavingsGoalEvent.UpdateGoal -> {
                viewModelScope.launch {
                    useCases.updateSavingsGoal(event.goal)
                }
            }
            is SavingsGoalEvent.DeleteGoal -> {
                viewModelScope.launch {
                    useCases.deleteSavingsGoal(event.goal)
                }
            }
            is SavingsGoalEvent.AddMoney -> {
                viewModelScope.launch {
                    useCases.addMoneyToGoal(event.id, event.amount)
                }
            }
            is SavingsGoalEvent.WithdrawMoney -> {
                viewModelScope.launch {
                    useCases.withdrawMoneyFromGoal(event.id, event.amount)
                }
            }
            is SavingsGoalEvent.UpdateStatus -> {
                viewModelScope.launch {
                    useCases.updateGoalStatus(event.id, event.status)
                }
            }
            is SavingsGoalEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is SavingsGoalEvent.OnSortTypeChange -> {
                _sortBy.value = event.sortType
            }
        }
    }

    enum class SortType {
        DEADLINE, AMOUNT, PROGRESS
    }
}
