package com.portfolio.financetracker.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.portfolio.financetracker.ui.calendar.CalendarScreen
import com.portfolio.financetracker.ui.charts.ChartsScreen
import com.portfolio.financetracker.ui.dashboard.DashboardScreen
import com.portfolio.financetracker.ui.transaction.AddTransactionScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FinanceNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SplashScreen.route,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val drawerState  = rememberDrawerState(DrawerValue.Closed)
    val scope        = rememberCoroutineScope()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val userProfile  by authViewModel.currentUser.collectAsState()

    // Screens that should NOT show the drawer (auth flow)
    val noDrawerScreens = listOf(
        Screen.LoginScreen.route,
        Screen.SplashScreen.route,
        Screen.VerifyEmailScreen.route
    )
    val drawerEnabled = currentRoute !in noDrawerScreens

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
            // ── Splash ────────────────────────────────────────────────────────
            composable(route = Screen.SplashScreen.route) {
                com.portfolio.financetracker.ui.splash.SplashScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Screen.DashboardScreen.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    },
                    onNavigateToVerifyEmail = {
                        navController.navigate(Screen.VerifyEmailScreen.route) {
                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Login / Register ──────────────────────────────────────────────
            composable(route = Screen.LoginScreen.route) {
                // Navigation is driven entirely by the ViewModel's eventFlow.
                // The onAuthSuccess callback in LoginScreen is kept as a no-op
                // to avoid double-navigation.
                LaunchedEffect(Unit) {
                    authViewModel.eventFlow.collect { event ->
                        when (event) {
                            is AuthViewModel.UiEvent.NavigateToHome -> {
                                navController.navigate(Screen.DashboardScreen.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthViewModel.UiEvent.NavigateToVerifyEmail -> {
                                navController.navigate(Screen.VerifyEmailScreen.route) {
                                    popUpTo(Screen.LoginScreen.route) { inclusive = false }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                LoginScreen(
                    // onAuthSuccess is intentionally a no-op here; navigation is
                    // handled above via eventFlow to avoid duplicate back-stack entries.
                    onAuthSuccess = {}
                )
            }

            // ── Verify Email ──────────────────────────────────────────────────
            composable(route = Screen.VerifyEmailScreen.route) {
                com.portfolio.financetracker.ui.auth.VerifyEmailScreen(
                    onVerified = {
                        navController.navigate(Screen.DashboardScreen.route) {
                            popUpTo(Screen.VerifyEmailScreen.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
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
                    onNavigateToTransactions = {
                        navController.navigate(Screen.TransactionsScreen.route)
                    },
                    onNavigateToCharts = { navController.navigate(Screen.ChartsScreen.route) },
                    onNavigateToSettings = { navController.navigate(Screen.SettingsScreen.route) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // ── All Transactions ──────────────────────────────────────────────
            composable(route = Screen.TransactionsScreen.route) {
                com.portfolio.financetracker.ui.dashboard.TransactionsScreen(
                    onNavigateToAddTransaction = { transactionId ->
                        val route = if (transactionId != null)
                            "${Screen.AddEditTransactionScreen.route}?transactionId=$transactionId"
                        else Screen.AddEditTransactionScreen.route
                        navController.navigate(route)
                    },
                    onNavigateBack = { navController.popBackStack() }
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

            // ── Calendar & Reminders ──────────────────────────────────────────
            composable(route = Screen.CalendarScreen.route) {
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // ── Insights ──────────────────────────────────────────────────────
            composable(route = Screen.InsightsScreen.route) {
                PlaceholderScreen(
                    title = "Insights",
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Bank Accounts ─────────────────────────────────────────────────
            composable(route = Screen.BankAccountsScreen.route) {
                com.portfolio.financetracker.ui.banks.BankAccountsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBankClick    = { bankName ->
                        navController.navigate(Screen.BankTransactionsScreen.createRoute(bankName))
                    }
                )
            }

            // ── Bank Transactions ─────────────────────────────────────────────
            composable(
                route = Screen.BankTransactionsScreen.route,
                arguments = listOf(
                    navArgument("bankName") { type = NavType.StringType }
                )
            ) {
                com.portfolio.financetracker.ui.banks.BankTransactionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("$title — coming soon")
        }
    }
}
