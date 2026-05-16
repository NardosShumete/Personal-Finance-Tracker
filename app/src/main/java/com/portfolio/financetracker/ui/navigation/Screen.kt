package com.portfolio.financetracker.ui.navigation

sealed class Screen(val route: String) {
    object LoginScreen : Screen("login_screen")
    object DashboardScreen : Screen("dashboard_screen")
    object AddEditTransactionScreen : Screen("add_edit_transaction_screen")
    object ChartsScreen : Screen("charts_screen")
    object SettingsScreen : Screen("settings_screen")
    object MonthlyGoalsScreen : Screen("monthly_goals_screen")
    object AboutUsScreen : Screen("about_us_screen")
    object PendingReviewScreen : Screen("pending_review_screen")
    object SmsAccountSetupScreen : Screen("sms_account_setup_screen")
    object CalendarScreen : Screen("calendar_screen")
}
