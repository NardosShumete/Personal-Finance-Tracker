package com.portfolio.financetracker.ui.dashboard

import com.portfolio.financetracker.domain.model.Transaction

sealed class DashboardEvent {
    data class OnSearchQueryChanged(val query: String): DashboardEvent()
    data class DeleteTransaction(val transaction: Transaction): DashboardEvent()
    data class OnPeriodChanged(val period: SummaryPeriod): DashboardEvent()
}
