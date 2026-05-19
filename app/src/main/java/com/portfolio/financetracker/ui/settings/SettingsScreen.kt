package com.portfolio.financetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import com.portfolio.financetracker.R
import com.portfolio.financetracker.core.util.CurrencyHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMonthlyGoals: () -> Unit = {},
    onNavigateToAboutUs: () -> Unit = {},
    onNavigateToSmsSetup: () -> Unit = {},
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
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                text = stringResource(R.string.security),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // ── SMS Auto-Parse card ───────────────────────────────────────────
            SmsPermissionCard(
                onManageAccounts = onNavigateToSmsSetup
            )

            Spacer(modifier = Modifier.height(16.dp))
            
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
                            Text(text = stringResource(R.string.biometric_lock), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.biometric_desc),
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
                text = stringResource(R.string.preferences),
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
                            Text(text = stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.dark_mode_desc),
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
                    title = { Text(stringResource(R.string.select_currency)) },
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
                            Text(stringResource(R.string.cancel))
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
                            Text(text = stringResource(R.string.main_currency), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.current_currency, currentCurrency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Toggle
            var showLanguageDialog by remember { mutableStateOf(false) }
            val currentLanguage by viewModel.languageCode.collectAsState()
            val languageOptions = listOf(Pair("en", "English"), Pair("am", "አማርኛ"))

            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.select_language)) },
                    text = {
                        LazyColumn {
                            items(languageOptions) { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setLanguageCode(lang.first)
                                            showLanguageDialog = false
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(text = lang.second, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = "Language")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.current_language, if (currentLanguage == "am") "አማርኛ" else "English"),
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
                            Text(text = stringResource(R.string.monthly_goals), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.monthly_goals_desc),
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
                text = stringResource(R.string.about_us),
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
                            Text(text = stringResource(R.string.about_us), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.about_us_desc),
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
                text = stringResource(R.string.data_management),
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
                            Text(text = stringResource(R.string.export_data), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.export_data_desc),
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
