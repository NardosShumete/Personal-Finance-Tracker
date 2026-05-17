package com.portfolio.financetracker.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.data.local.entity.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar & Reminders") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reminders set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(reminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            onToggle = { viewModel.toggleCompletion(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddReminderDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, date, type, category, interval, autoGen, sync ->
                    viewModel.addReminder(title, amount, date, type, category, interval, autoGen, sync)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(reminder.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggle() }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (reminder.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${reminder.category} • ${reminder.type} • $dateString",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.repeatInterval != "NONE") {
                        Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(" ${reminder.repeatInterval}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (reminder.autoGenerateExpense) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2ECC71))
                        Text(" Auto-pay", fontSize = 12.sp, color = Color(0xFF2ECC71))
                    }
                }
                Text(
                    text = "Amount: ${reminder.amount}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, String, String, String, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("RENT") }
    var category by remember { mutableStateOf("Housing") }
    var interval by remember { mutableStateOf("MONTHLY") }
    var autoGen by remember { mutableStateOf(true) }
    var sync by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val date by remember { mutableLongStateOf(calendar.timeInMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Monthly Reminder") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Yebet Kiray)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Category", style = MaterialTheme.typography.labelMedium)
                val categories = listOf("Housing", "Utilities", "Subscription", "Debt", "Investment")
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunkedCategories = categories.chunked(2)
                    chunkedCategories.forEach { chunk ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunk.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoGen, onCheckedChange = { autoGen = it })
                    Text("Auto-generate Expense", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sync, onCheckedChange = { sync = it })
                    Text("Sync with Google Calendar", style = MaterialTheme.typography.bodyMedium)
                }

                Text("Repeat", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = interval == "NONE", onClick = { interval = "NONE" }, label = { Text("Once") })
                    FilterChip(selected = interval == "WEEKLY", onClick = { interval = "WEEKLY" }, label = { Text("Weekly") })
                    FilterChip(selected = interval == "MONTHLY", onClick = { interval = "MONTHLY" }, label = { Text("Monthly") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, amount.toDoubleOrNull() ?: 0.0, date, type, category, interval, autoGen, sync)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
