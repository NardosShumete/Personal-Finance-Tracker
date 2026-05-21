package com.portfolio.financetracker.ui.savings

import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus

sealed class SavingsGoalEvent {
    data class AddGoal(val goal: SavingsGoal) : SavingsGoalEvent()
    data class UpdateGoal(val goal: SavingsGoal) : SavingsGoalEvent()
    data class DeleteGoal(val goal: SavingsGoal) : SavingsGoalEvent()
    data class AddMoney(val id: Int, val amount: Double) : SavingsGoalEvent()
    data class WithdrawMoney(val id: Int, val amount: Double) : SavingsGoalEvent()
    data class UpdateStatus(val id: Int, val status: SavingsGoalStatus) : SavingsGoalEvent()
    data class OnSearchQueryChange(val query: String) : SavingsGoalEvent()
    data class OnSortTypeChange(val sortType: SavingsGoalViewModel.SortType) : SavingsGoalEvent()
}
