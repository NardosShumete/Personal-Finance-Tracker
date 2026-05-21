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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.financetracker.data.local.entity.BankAccountEntity

// ── Add Bank Bottom Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankBottomSheet(
    onDismiss: () -> Unit,
    onAddBank: (String, String, String) -> Unit
) {
    var shortName   by remember { mutableStateOf("") }
    var fullName    by remember { mutableStateOf("") }
    var smsSenderId by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Add Bank Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value         = shortName,
                onValueChange = { shortName = it },
                label         = { Text("Short Name (e.g. CBE)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value         = fullName,
                onValueChange = { fullName = it },
                label         = { Text("Full Bank Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value         = smsSenderId,
                onValueChange = { smsSenderId = it },
                label         = { Text("SMS Sender ID (e.g. CBEBirr)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (shortName.isNotBlank() && smsSenderId.isNotBlank()) {
                            onAddBank(shortName.trim(), fullName.trim(), smsSenderId.trim())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled  = shortName.isNotBlank() && smsSenderId.isNotBlank()
                ) {
                    Text("Add")
                }
            }
        }
    }
}

// ── Accounts Section ──────────────────────────────────────────────────────────

@Composable
fun BankAccountsSection(
    viewModel: BankAccountViewModel,
    onAddBankClick: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val expandedBankId by viewModel.expandedBankId.collectAsState()
    val bankToDelete   by viewModel.bankToDelete.collectAsState()

    // ── Delete confirmation dialog ────────────────────────────────────────────
    bankToDelete?.let { bank ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text       = "Remove ${bank.shortName}?",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text  = "This removes the \"${bank.fullName}\" card from your dashboard. " +
                            "Your existing transactions will not be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "Accounts",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        // 2-column grid — heightIn avoids infinite-height crash inside LazyColumn
        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.heightIn(max = 2000.dp)
        ) {
            items(bankAccounts, key = { it.id }) { bank ->
                BankCard(
                    bank       = bank,
                    isExpanded = expandedBankId == bank.id,
                    onTap      = { viewModel.toggleExpand(bank.id) },
                    onConnect  = { viewModel.toggleConnect(bank.id) },
                    onDelete   = { viewModel.requestDelete(bank) }
                )
            }
            item {
                AddBankCard(onClick = onAddBankClick)
            }
        }
    }
}

// ── Bank Card ─────────────────────────────────────────────────────────────────

@Composable
fun BankCard(
    bank: BankAccountEntity,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue  = if (isExpanded) 180f else 0f,
        label        = "chevron"
    )

    // Show lastKnownBalance (real SMS balance) when available,
    // fall back to income-minus-expense only when no SMS balance exists yet.
    val displayBalance = bank.lastKnownBalance ?: (bank.totalIncome - bank.totalExpense)
    val hasRealBalance = bank.lastKnownBalance != null

    val accentColor = runCatching {
        Color(android.graphics.Color.parseColor(bank.colorHex))
    }.getOrDefault(Color(0xFF2ECC71))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onTap() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header row: dot + name | X button + chevron ───────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                // Left: colour dot + short name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = bank.shortName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1
                    )
                }

                // Right: X delete button + chevron
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // X button — small, red, always visible
                    IconButton(
                        onClick  = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Remove ${bank.shortName}",
                            tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier           = Modifier.size(16.dp)
                        )
                    }

                    // Chevron — expands/collapses detail
                    Icon(
                        imageVector        = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier           = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Balance ───────────────────────────────────────────────────────
            Text(
                text       = "ETB ${String.format("%.2f", displayBalance)}",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color      = when {
                    displayBalance > 0 -> Color(0xFF2ECC71)
                    displayBalance < 0 -> MaterialTheme.colorScheme.error
                    else               -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Label: "Balance" when real SMS balance, "Net" when calculated
            Text(
                text  = if (hasRealBalance) "Balance" else "Net (no SMS yet)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // ── Status + transaction count ────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(top = 4.dp)
            ) {
                StatusBadge(isConnected = bank.isConnected)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "${bank.transactionCount} trans.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Expanded detail ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("Full Name", bank.fullName)
                    DetailRow("Sender ID", bank.smsSenderId)
                    if (hasRealBalance) {
                        DetailRow(
                            "Last SMS Balance",
                            "ETB ${String.format("%.2f", bank.lastKnownBalance!!)}",
                            Color(0xFF2ECC71)
                        )
                    }
                    DetailRow("Total In",  "ETB ${String.format("%.2f", bank.totalIncome)}",  Color(0xFF2E7D32))
                    DetailRow("Total Out", "ETB ${String.format("%.2f", bank.totalExpense)}", Color(0xFFC62828))

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick          = onConnect,
                        modifier         = Modifier.fillMaxWidth(),
                        colors           = ButtonDefaults.buttonColors(
                            containerColor = if (bank.isConnected)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            contentColor   = if (bank.isConnected)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape            = RoundedCornerShape(8.dp),
                        contentPadding   = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(
                            if (bank.isConnected) "Disconnect SMS" else "Connect SMS",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
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
            Text(
                "Disconnected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Detail row ────────────────────────────────────────────────────────────────

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color      = valueColor
        )
    }
}

// ── Add bank card ─────────────────────────────────────────────────────────────

@Composable
fun AddBankCard(onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border    = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Bank",
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add Bank",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
