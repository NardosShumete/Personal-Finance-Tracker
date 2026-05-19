package com.portfolio.financetracker.ui.settings.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.domain.model.CategoryBudget
import com.portfolio.financetracker.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyGoalsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyGoalsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyCode = LocalCurrencyCode.current

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is MonthlyGoalsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is MonthlyGoalsViewModel.UiEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(message = "Budget & Goals saved")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner & Alerts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(MonthlyGoalsEvent.ResetBudgets) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Summary Overview ──
                item {
                    BudgetOverviewCard(
                        totalBudget = state.expenseLimit.toDoubleOrNull() ?: 0.0,
                        spent = state.totalSpent,
                        currencyCode = currencyCode,
                        insightMessage = state.insightMessage
                    )
                }

                // ── Main Budget Settings ──
                item {
                    MainBudgetSettings(
                        incomeGoal = state.incomeGoal,
                        expenseLimit = state.expenseLimit,
                        alertsEnabled = state.isBudgetAlertsEnabled,
                        onIncomeChange = { viewModel.onEvent(MonthlyGoalsEvent.EnteredIncomeGoal(it)) },
                        onExpenseChange = { viewModel.onEvent(MonthlyGoalsEvent.EnteredExpenseLimit(it)) },
                        onAlertToggle = { viewModel.onEvent(MonthlyGoalsEvent.ToggleBudgetAlerts(it)) }
                    )
                }

                item {
                    Text(
                        text = "Category Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // ── Category Budgets ──
                items(state.categoryBudgets) { budget ->
                    CategoryBudgetCard(
                        budget = budget,
                        currencyCode = currencyCode,
                        onLimitChange = { viewModel.onEvent(MonthlyGoalsEvent.EnteredCategoryLimit(budget.category, it)) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.onEvent(MonthlyGoalsEvent.SaveGoals) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Budget & Limits", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun BudgetOverviewCard(
    totalBudget: Double,
    spent: Double,
    currencyCode: String,
    insightMessage: String
) {
    val remaining = totalBudget - spent
    val progress = if (totalBudget > 0) (spent / totalBudget).coerceIn(0.0, 1.0).toFloat() else 0f
    
    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 1.0f -> Color.Red
            progress >= 0.8f -> Color(0xFFFFC107) // Yellow
            else -> Color(0xFF4CAF50) // Green
        }, label = "color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyHelper.formatAmount(spent, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyHelper.formatAmount(remaining, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining < 0) Color.Red else Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = animateFloatAsState(targetValue = progress, label = "progress").value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(insightMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MainBudgetSettings(
    incomeGoal: String,
    expenseLimit: String,
    alertsEnabled: Boolean,
    onIncomeChange: (String) -> Unit,
    onExpenseChange: (String) -> Unit,
    onAlertToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = incomeGoal,
                onValueChange = onIncomeChange,
                label = { Text("Monthly Income Target") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = expenseLimit,
                onValueChange = onExpenseChange,
                label = { Text("Total Spending Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Budget Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Smart notifications for overspending", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = alertsEnabled, onCheckedChange = onAlertToggle)
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    budget: CategoryBudget,
    currencyCode: String,
    onLimitChange: (String) -> Unit
) {
    val progress = budget.progress
    val progressColor = when {
        progress >= 1.0f -> Color.Red
        progress >= 0.8f -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                
                OutlinedTextField(
                    value = if (budget.limitAmount > 0) budget.limitAmount.toString() else "",
                    onValueChange = onLimitChange,
                    placeholder = { Text("Set Limit") },
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = animateFloatAsState(targetValue = progress, label = "progress").value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Spent: ${CurrencyHelper.formatAmount(budget.spentAmount, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Remaining: ${CurrencyHelper.formatAmount(budget.remainingAmount, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (budget.remainingAmount < 0) Color.Red else Color.Unspecified
                )
            }
        }
    }
}
