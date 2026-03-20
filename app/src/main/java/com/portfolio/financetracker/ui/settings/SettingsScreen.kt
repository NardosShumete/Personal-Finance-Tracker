package com.portfolio.financetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.ui.auth.BiometricViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.portfolio.financetracker.core.util.CurrencyHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMonthlyGoals: () -> Unit = {},
    onNavigateToAboutUs: () -> Unit = {},
    viewModel: BiometricViewModel = hiltViewModel()
) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val csvData = viewModel.createCsvData()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvData.toByteArray())
                    }
                    snackbarHostState.showSnackbar("Export successful!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Biometric Lock", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Require fingerprint to open app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Dark Mode Toggle
            val isDarkMode by viewModel.isDarkModeEnabled.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val switchChecked = isDarkMode ?: isSystemDark

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = "Dark Mode")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Dark Mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Toggle application theme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = switchChecked,
                        onCheckedChange = { viewModel.setDarkModeEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Currency Toggle
            var showCurrencyDialog by remember { mutableStateOf(false) }
            val currentCurrency by viewModel.currencyCode.collectAsState()
            
            if (showCurrencyDialog) {
                AlertDialog(
                    onDismissRequest = { showCurrencyDialog = false },
                    title = { Text("Select Currency") },
                    text = {
                        LazyColumn {
                            items(CurrencyHelper.supportedCurrencies) { currency ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setCurrencyCode(currency.first)
                                            showCurrencyDialog = false
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(text = "${currency.first} - ${currency.second}", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCurrencyDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrencyDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Currency")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Main Currency", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Current: $currentCurrency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMonthlyGoals() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = "Goals")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Monthly Goals", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Set your budget and income targets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAboutUs() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "About Us")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "About Us", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Developer info, feedback & privacy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { csvExportLauncher.launch("finance_data.csv") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Export CSV")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Export Data (CSV)", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Save your transactions to device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Export")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
