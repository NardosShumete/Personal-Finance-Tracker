package com.portfolio.financetracker.ui.navigation

sealed class Screen(val route: String) {
    object DashboardScreen : Screen("dashboard_screen")
    object AddEditTransactionScreen : Screen("add_edit_transaction_screen")
    object ChartsScreen : Screen("charts_screen")
}
