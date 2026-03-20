package com.portfolio.financetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.portfolio.financetracker.ui.charts.ChartsScreen
import com.portfolio.financetracker.ui.dashboard.DashboardScreen
import com.portfolio.financetracker.ui.transaction.AddTransactionScreen

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
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddEditTransactionScreen.route)
                },
                onNavigateToCharts = {
                    navController.navigate(Screen.ChartsScreen.route)
                }
            )
        }
        composable(route = Screen.AddEditTransactionScreen.route) {
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
    }
}
