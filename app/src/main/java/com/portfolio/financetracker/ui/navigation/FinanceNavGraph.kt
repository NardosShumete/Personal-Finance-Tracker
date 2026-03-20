package com.portfolio.financetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.portfolio.financetracker.ui.charts.ChartsScreen
import com.portfolio.financetracker.ui.dashboard.DashboardScreen
import com.portfolio.financetracker.ui.transaction.AddTransactionScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun FinanceNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.DashboardScreen.route
    ) {
        composable(route = Screen.DashboardScreen.route) {
            DashboardScreen(
                onNavigateToAddTransaction = { transactionId ->
                    val route = if (transactionId != null) {
                        "${Screen.AddEditTransactionScreen.route}?transactionId=$transactionId"
                    } else {
                        Screen.AddEditTransactionScreen.route
                    }
                    navController.navigate(route)
                },
                onNavigateToCharts = {
                    navController.navigate(Screen.ChartsScreen.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.SettingsScreen.route)
                }
            )
        }
        composable(
            route = Screen.AddEditTransactionScreen.route + "?transactionId={transactionId}",
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.ChartsScreen.route) {
            ChartsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.SettingsScreen.route) {
            com.portfolio.financetracker.ui.settings.SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMonthlyGoals = {
                    navController.navigate(Screen.MonthlyGoalsScreen.route)
                },
                onNavigateToAboutUs = {
                    navController.navigate(Screen.AboutUsScreen.route)
                }
            )
        }
        composable(route = Screen.MonthlyGoalsScreen.route) {
            com.portfolio.financetracker.ui.settings.goals.MonthlyGoalsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.AboutUsScreen.route) {
            com.portfolio.financetracker.ui.settings.about.AboutUsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
