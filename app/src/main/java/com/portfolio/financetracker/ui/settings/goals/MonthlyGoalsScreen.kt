package com.portfolio.financetracker.ui.settings.goals

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyGoalsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyGoalsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyCode = LocalCurrencyCode.current
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is MonthlyGoalsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is MonthlyGoalsViewModel.UiEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(message = "Budget configuration saved!")
                    isEditing = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Budget Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(state.monthYear, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(MonthlyGoalsEvent.ResetBudgets) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isEditing) {
                ExtendedFloatingActionButton(
                    onClick = { isEditing = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Setup Budget", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        WelcomeHeader()
                    }

                    item {
                        BudgetOverviewCard(
                            totalBudget = state.expenseLimit.toDoubleOrNull() ?: 0.0,
                            spent = state.totalSpent,
                            currencyCode = currencyCode,
                            insightMessage = state.insightMessage
                        )
                    }

                    item {
                        Text(
                            text = "Planning Services",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    items(state.categoryBudgets) { budget ->
                        CategoryServiceCard(
                            budget = budget,
                            currencyCode = currencyCode
                        )
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            if (isEditing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BudgetConfigurationForm(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onClose = { isEditing = false }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(
            text = "Welcome to FinTrack",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Budget Monitoring Active",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldGreen,
                fontWeight = FontWeight.Bold
            )
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
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 1.0f -> ElectricRose
            progress >= 0.8f -> Color(0xFFFFC107)
            else -> EmeraldGreen
        }, label = "color"
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        CurrencyHelper.formatAmount(spent, currencyCode),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (remaining >= 0) "SAFE" else "LIMIT EXCEEDED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (remaining >= 0) EmeraldGreen else ElectricRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape),
                color = progressColor,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Remaining: ${CurrencyHelper.formatAmount(remaining, currencyCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(progress * 100).toInt()}% Used",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        insightMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryServiceCard(
    budget: CategoryBudget,
    currencyCode: String
) {
    val progress = budget.progress
    val statusColor = when {
        progress >= 1.0f -> ElectricRose
        progress >= 0.8f -> Color(0xFFFFC107)
        else -> EmeraldGreen
    }

    val iconData = when(budget.category.lowercase()) {
        "food" -> Pair(Icons.Default.Restaurant, Color(0xFFF44336))
        "transport" -> Pair(Icons.Default.DirectionsCar, Color(0xFF2196F3))
        "bills" -> Pair(Icons.Default.ReceiptLong, Color(0xFFFF9800))
        "shopping" -> Pair(Icons.Default.ShoppingBag, Color(0xFF9C27B0))
        "entertainment" -> Pair(Icons.Default.Movie, Color(0xFFE91E63))
        "savings" -> Pair(Icons.Default.Savings, Color(0xFF4CAF50))
        else -> Pair(Icons.Default.Category, Color(0xFF607D8B))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(iconData.second.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconData.first, null, tint = iconData.second, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(budget.category, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Limit: ${CurrencyHelper.formatAmount(budget.limitAmount, currencyCode)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    CurrencyHelper.formatAmount(budget.spentAmount, currencyCode),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { animateFloatAsState(targetValue = progress, label = "progress").value },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )
            
            if (progress >= 1.0f) {
                Text(
                    "Exceeded by ${CurrencyHelper.formatAmount(-budget.remainingAmount, currencyCode)}",
                    color = ElectricRose,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetConfigurationForm(
    state: MonthlyGoalsState,
    onEvent: (MonthlyGoalsEvent) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configure Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Income & Spending Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.incomeGoal,
                    onValueChange = { onEvent(MonthlyGoalsEvent.EnteredIncomeGoal(it)) },
                    label = { Text("Monthly Income Target (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.TrendingUp, null, tint = EmeraldGreen) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.expenseLimit,
                    onValueChange = { onEvent(MonthlyGoalsEvent.EnteredExpenseLimit(it)) },
                    label = { Text("Max Spending Limit (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.TrendingDown, null, tint = ElectricRose) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budget Alerts", fontWeight = FontWeight.Bold)
                            Text("Notify me on threshold hits", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = state.isBudgetAlertsEnabled, onCheckedChange = { onEvent(MonthlyGoalsEvent.ToggleBudgetAlerts(it)) })
                    }
                }
            }

            item {
                Text("Category Spending Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(state.categoryBudgets) { budget ->
                OutlinedTextField(
                    value = if (budget.limitAmount > 0) budget.limitAmount.toString() else "",
                    onValueChange = { onEvent(MonthlyGoalsEvent.EnteredCategoryLimit(budget.category, it)) },
                    label = { Text("${budget.category} Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    prefix = { Text("Br ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                )
            }

            item {
                Button(
                    onClick = { onEvent(MonthlyGoalsEvent.SaveGoals) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Configuration", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
