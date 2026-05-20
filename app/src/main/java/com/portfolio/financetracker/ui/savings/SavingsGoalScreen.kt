package com.portfolio.financetracker.ui.savings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.R
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.domain.manager.AchievementManager
import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.ui.savings.components.*
import com.portfolio.financetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: SavingsGoalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currencyCode = LocalCurrencyCode.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var selectedGoalForAction by remember { mutableStateOf<SavingsGoal?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Savings Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Deadline") },
                                onClick = { 
                                    viewModel.onEvent(SavingsGoalEvent.OnSortTypeChange(SavingsGoalViewModel.SortType.DEADLINE))
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount") },
                                onClick = { 
                                    viewModel.onEvent(SavingsGoalEvent.OnSortTypeChange(SavingsGoalViewModel.SortType.AMOUNT))
                                    showSortMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress") },
                                onClick = { 
                                    viewModel.onEvent(SavingsGoalEvent.OnSortTypeChange(SavingsGoalViewModel.SortType.PROGRESS))
                                    showSortMenu = false 
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    goalToEdit = null
                    showAddDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SavingsSummarySection(state, currencyCode)

            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(SavingsGoalEvent.OnSearchQueryChange(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.goals.isEmpty() && state.searchQuery.isEmpty()) {
                EmptySavingsState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AchievementSection(
                            achievements = AchievementManager.calculateAchievements(state.goals)
                        )
                    }

                    if (state.goals.isNotEmpty()) {
                        item {
                            Text(
                                "Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            SavingsPieChart(
                                goals = state.goals,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            "My Goals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    items(state.goals, key = { it.id }) { goal ->
                        SavingsGoalCard(
                            goal = goal,
                            currencyCode = currencyCode,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { onNavigateToDetail(goal.id) },
                            onMenuClick = {
                                selectedGoalForAction = goal
                                showActionMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSavingsGoalDialog(
            goal = goalToEdit,
            onDismiss = { showAddDialog = false },
            onConfirm = { goal ->
                if (goalToEdit == null) {
                    viewModel.onEvent(SavingsGoalEvent.AddGoal(goal))
                } else {
                    viewModel.onEvent(SavingsGoalEvent.UpdateGoal(goal))
                }
                showAddDialog = false
            }
        )
    }
    
    if (showActionMenu && selectedGoalForAction != null) {
        SavingsGoalActionDialog(
            goal = selectedGoalForAction!!,
            onDismiss = { showActionMenu = false },
            onAddMoney = { amount -> 
                viewModel.onEvent(SavingsGoalEvent.AddMoney(selectedGoalForAction!!.id, amount))
            },
            onWithdraw = { amount ->
                viewModel.onEvent(SavingsGoalEvent.WithdrawMoney(selectedGoalForAction!!.id, amount))
            },
            onEdit = {
                goalToEdit = selectedGoalForAction
                showAddDialog = true
                showActionMenu = false
            },
            onDelete = {
                viewModel.onEvent(SavingsGoalEvent.DeleteGoal(selectedGoalForAction!!))
            }
        )
    }
}

@Composable
fun SavingsSummarySection(state: SavingsGoalState, currencyCode: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Savings",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = CurrencyHelper.formatAmount(state.totalSavings, currencyCode),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Active", value = state.activeGoalsCount.toString(), icon = Icons.Default.PlayArrow)
                Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)))
                StatItem(label = "Completed", value = state.completedGoalsCount.toString(), icon = Icons.Default.CheckCircle)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search goals...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun EmptySavingsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No savings goals yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Start saving for your dreams today!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SavingsGoalActionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onAddMoney: (Double) -> Unit,
    onWithdraw: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.title) },
        text = {
            Column {
                TabRow(selectedTabIndex = if (isAdding) 0 else 1) {
                    Tab(selected = isAdding, onClick = { isAdding = true }) {
                        Text("Add Money", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = !isAdding, onClick = { isAdding = false }) {
                        Text("Withdraw", modifier = Modifier.padding(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                Button(onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (isAdding) onAddMoney(amount) else onWithdraw(amount)
                    onDismiss()
                }) {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete Goal")
            }
        }
    )
}
