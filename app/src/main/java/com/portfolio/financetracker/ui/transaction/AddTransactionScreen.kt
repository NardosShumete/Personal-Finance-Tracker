package com.portfolio.financetracker.ui.transaction

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(AddTransactionEvent.ChangedReceipt(it.toString())) }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddTransactionViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is AddTransactionViewModel.UiEvent.SaveSuccess -> {
                    onNavigateBack()
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onEvent(AddTransactionEvent.ChangedDate(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to permanently delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(AddTransactionEvent.DeleteTransaction)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                types.forEach { type ->
                    val isSelected = state.type == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onEvent(AddTransactionEvent.ChangedType(type)) },
                        label = { Text(type.name) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(AddTransactionEvent.EnteredAmount(it)) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("Br ") }
            )

            // Category
            OutlinedTextField(
                value = state.category,
                onValueChange = { viewModel.onEvent(AddTransactionEvent.EnteredCategory(it)) },
                label = { Text("Category (e.g. Groceries)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Date Picker Trigger
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(state.date))
                    Text(text = "Date: $formattedDate", style = MaterialTheme.typography.bodyLarge)
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                }
            }

            // Recurring Period
            Text(text = "Recurring Payment", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth()) {
                RecurringPeriod.values().forEach { period ->
                    FilterChip(
                        selected = state.recurringPeriod == period,
                        onClick = { viewModel.onEvent(AddTransactionEvent.ChangedRecurring(period)) },
                        label = { Text(period.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Receipt Photo Launcher
            OutlinedButton(
                onClick = { photoLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.receiptPath == null) "Attach Receipt Photo" else "Receipt Attached ✅")
            }

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.onEvent(AddTransactionEvent.EnteredNote(it)) },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onEvent(AddTransactionEvent.SaveTransaction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (state.id == null) "Save Transaction" else "Update Transaction", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
