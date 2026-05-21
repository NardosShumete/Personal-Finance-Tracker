package com.portfolio.financetracker.ui.savings.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.financetracker.data.local.dao.SavingsGoalTransactionDao
import com.portfolio.financetracker.data.mapper.toDomain
import com.portfolio.financetracker.domain.use_case.SavingsGoalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsGoalDetailViewModel @Inject constructor(
    private val useCases: SavingsGoalUseCases,
    private val transactionDao: SavingsGoalTransactionDao
) : ViewModel() {

    private val _state = MutableStateFlow(SavingsGoalDetailState())
    val state: StateFlow<SavingsGoalDetailState> = _state.asStateFlow()

    fun loadGoal(goalId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val goal = useCases.getSavingsGoalById(goalId)
            if (goal != null) {
                // Combine goal details with transaction history
                transactionDao.getTransactionsByGoal(goalId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .onEach { transactions ->
                        _state.update { it.copy(
                            goal = goal,
                            transactions = transactions,
                            isLoading = false
                        ) }
                    }
                    .launchIn(viewModelScope)
            } else {
                _state.update { it.copy(isLoading = false, error = "Goal not found") }
            }
        }
    }
}
