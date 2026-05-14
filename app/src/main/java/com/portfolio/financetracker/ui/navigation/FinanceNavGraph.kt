package com.portfolio.financetracker.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.portfolio.financetracker.ui.auth.AuthViewModel
import com.portfolio.financetracker.ui.auth.LoginScreen
import com.portfolio.financetracker.ui.charts.ChartsScreen
import com.portfolio.financetracker.ui.dashboard.DashboardScreen
import com.portfolio.financetracker.ui.transaction.AddTransactionScreen
import kotlinx.coroutines.launch

@Composable
fun FinanceNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.DashboardScreen.route,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val drawerState  = rememberDrawerState(DrawerValue.Closed)
    val scope        = rememberCoroutineScope()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val userProfile  by authViewModel.currentUser.collectAsState()

    // Screens that should NOT show the drawer (auth flow)
    val drawerEnabled = currentRoute != Screen.LoginScreen.route

    ModalNavigationDrawer(
        drawerState   = drawerState,
        gesturesEnabled = drawerEnabled,
        drawerContent = {
            AppDrawer(
                userProfile  = userProfile,
                currentRoute = currentRoute,
                onNavigateTo = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Screen.DashboardScreen.route)
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    authViewModel.signOut()
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    ) {
        NavHost(
            navController    = navController,
            startDestination = startDestination
        ) {
            // ── Login / Register ──────────────────────────────────────────────
            composable(route = Screen.LoginScreen.route) {
                LoginScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.DashboardScreen.route) {
                            popUpTo(Screen.LoginScreen.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Dashboard ─────────────────────────────────────────────────────
            composable(route = Screen.DashboardScreen.route) {
                DashboardScreen(
                    onNavigateToAddTransaction = { transactionId ->
                        val route = if (transactionId != null)
                            "${Screen.AddEditTransactionScreen.route}?transactionId=$transactionId"
                        else Screen.AddEditTransactionScreen.route
                        navController.navigate(route)
                    },
                    onNavigateToCharts = { navController.navigate(Screen.ChartsScreen.route) },
                    onNavigateToSettings = { navController.navigate(Screen.SettingsScreen.route) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // ── Add / Edit Transaction ────────────────────────────────────────
            composable(
                route = Screen.AddEditTransactionScreen.route + "?transactionId={transactionId}",
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) {
                AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Charts ────────────────────────────────────────────────────────
            composable(route = Screen.ChartsScreen.route) {
                ChartsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Settings ──────────────────────────────────────────────────────
            composable(route = Screen.SettingsScreen.route) {
                com.portfolio.financetracker.ui.settings.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMonthlyGoals = { navController.navigate(Screen.MonthlyGoalsScreen.route) },
                    onNavigateToAboutUs = { navController.navigate(Screen.AboutUsScreen.route) },
                    onNavigateToSmsSetup = { navController.navigate(Screen.SmsAccountSetupScreen.route) }
                )
            }

            // ── Monthly Goals ─────────────────────────────────────────────────
            composable(route = Screen.MonthlyGoalsScreen.route) {
                com.portfolio.financetracker.ui.settings.goals.MonthlyGoalsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── About Us ──────────────────────────────────────────────────────
            composable(route = Screen.AboutUsScreen.route) {
                com.portfolio.financetracker.ui.settings.about.AboutUsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Pending SMS Review ────────────────────────────────────────────
            composable(route = Screen.PendingReviewScreen.route) {
                com.portfolio.financetracker.ui.sms.PendingReviewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── SMS Account Setup ─────────────────────────────────────────────
            composable(route = Screen.SmsAccountSetupScreen.route) {
                com.portfolio.financetracker.ui.sms.SmsAccountSetupScreen(
                    onNavigateBack    = { navController.popBackStack() },
                    onSetupComplete   = { navController.popBackStack() }
                )
            }
        }
    }
}
