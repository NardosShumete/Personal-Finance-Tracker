package com.portfolio.financetracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.R
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.ui.dashboard.components.*
import com.portfolio.financetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: (Int?) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCharts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    bankViewModel: BankAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isDark = isSystemInDarkTheme()
    val currencyCode = LocalCurrencyCode.current
    
    var showAddBankSheet by remember { mutableStateOf(false) }
    var showSetGoalDialog by remember { mutableStateOf(false) }

    if (showAddBankSheet) {
        AddBankBottomSheet(
            onDismiss = { showAddBankSheet = false },
            onAddBank = { short, full, sender ->
                bankViewModel.addBank(short, full, sender)
                showAddBankSheet = false
            }
        )
    }

    if (showSetGoalDialog) {
        SetGoalDialog(
            initialIncomeGoal = state.monthlyGoal?.incomeGoal ?: 0.0,
            initialExpenseLimit = state.monthlyGoal?.expenseLimit ?: 0.0,
            onDismiss = { showSetGoalDialog = false },
            onConfirm = { income, expense ->
                viewModel.onEvent(DashboardEvent.SaveGoal(income, expense))
                showSetGoalDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GradientPurple.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TopBarIconButton(icon = Icons.Default.BarChart, onClick = onNavigateToCharts)
                            TopBarIconButton(icon = Icons.Default.Settings, onClick = onNavigateToSettings)
                        }
                    }
                }
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(EmeraldGreen, EmeraldDark))
                        )
                        .clickable { onNavigateToAddTransaction(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 16.dp, bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    SummaryCard(
                        state = state,
                        modifier = Modifier.clickable { showSetGoalDialog = true }
                    ) 
                }

                item {
                    SavingsSummaryCard(
                        totalSavings = state.totalSavings,
                        activeGoalsCount = state.activeSavingsGoalsCount,
                        currencyCode = currencyCode,
                        onClick = onNavigateToSavings
                    )
                }

                item {
                    BankAccountsSection(
                        viewModel = bankViewModel,
                        onAddBankClick = { showAddBankSheet = true }
                    )
                }

                item {
                    Button(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View All Transactions & Search")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
