package com.portfolio.financetracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.financetracker.data.local.entity.BankAccountEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankBottomSheet(
    onDismiss: () -> Unit,
    onAddBank: (String, String, String) -> Unit
) {
    var shortName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var smsSenderId by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Bank Account", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = shortName,
                onValueChange = { shortName = it },
                label = { Text("Bank Short Name (e.g. CBE)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Bank Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = smsSenderId,
                onValueChange = { smsSenderId = it },
                label = { Text("SMS Sender ID (e.g. CBEBirr)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (shortName.isNotBlank() && smsSenderId.isNotBlank()) {
                            onAddBank(shortName, fullName, smsSenderId)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = shortName.isNotBlank() && smsSenderId.isNotBlank()
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun BankAccountsSection(
    viewModel: BankAccountViewModel,
    onAddBankClick: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val expandedBankId by viewModel.expandedBankId.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 1000.dp) // Handle constrained height in LazyColumn
        ) {
            items(bankAccounts, key = { it.id }) { bank ->
                BankCard(
                    bank = bank,
                    isExpanded = expandedBankId == bank.id,
                    onTap = { viewModel.toggleExpand(bank.id) },
                    onConnect = { viewModel.toggleConnect(bank.id) }
                )
            }
            item {
                AddBankCard(onClick = onAddBankClick)
            }
        }
    }
}

@Composable
fun BankCard(
    bank: BankAccountEntity,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onConnect: () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    val balance = bank.totalIncome - bank.totalExpense

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(bank.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bank.shortName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).rotate(rotation)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ETB ${String.format("%.2f", balance)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                StatusBadge(isConnected = bank.isConnected)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${bank.transactionCount} txns",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DetailRow("Full Name", bank.fullName)
                    DetailRow("Sender ID", bank.smsSenderId)
                    DetailRow("Income", "ETB ${String.format("%.2f", bank.totalIncome)}", Color(0xFF2E7D32))
                    DetailRow("Expense", "ETB ${String.format("%.2f", bank.totalExpense)}", Color(0xFFC62828))

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bank.isConnected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (bank.isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(if (bank.isConnected) "Disconnect SMS" else "Connect SMS", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Live", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
        } else {
            Text("Disconnected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
fun AddBankCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Approximate height to match others
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add Bank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Utility extension for rotation
fun Modifier.rotate(degrees: Float) = this.then(
    androidx.compose.ui.draw.rotate(degrees)
)
