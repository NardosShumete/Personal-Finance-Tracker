package com.portfolio.financetracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.portfolio.financetracker.R
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.ui.dashboard.components.SummaryCard
import com.portfolio.financetracker.ui.dashboard.components.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: (Int?) -> Unit,
    onNavigateToCharts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Summary state — balance, totals, search query, goal
    val state by viewModel.state.collectAsState()

    // Paged transaction list — only loads what's visible on screen
    val pagedTransactions: LazyPagingItems<Transaction> =
        viewModel.pagedTransactions.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCharts) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "View Charts")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddTransaction(null) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary card uses the full-list state for accurate totals
            SummaryCard(state = state)

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(DashboardEvent.OnSearchQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search category or notes...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Empty state ───────────────────────────────────────────────
                if (pagedTransactions.itemCount == 0 &&
                    pagedTransactions.loadState.refresh !is LoadState.Loading
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Paged transaction items ───────────────────────────────────
                // itemKey { transaction.id } gives Compose a stable identity per
                // item so it can skip recomposition for unchanged rows.
                items(
                    count = pagedTransactions.itemCount,
                    key   = pagedTransactions.itemKey { it.id }
                ) { index ->
                    val transaction = pagedTransactions[index]
                    if (transaction != null) {
                        TransactionItem(
                            transaction  = transaction,
                            onDeleteClick = {
                                viewModel.onEvent(DashboardEvent.DeleteTransaction(transaction))
                            },
                            modifier = Modifier.clickable {
                                onNavigateToAddTransaction(transaction.id)
                            }
                        )
                    }
                }

                // ── Loading indicator (appended at bottom while next page loads)
                if (pagedTransactions.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // ── Error state ───────────────────────────────────────────────
                val refreshError = pagedTransactions.loadState.refresh
                if (refreshError is LoadState.Error) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error loading transactions. Tap to retry.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { pagedTransactions.retry() }
                            )
                        }
                    }
                }
            }
        }
    }
}
