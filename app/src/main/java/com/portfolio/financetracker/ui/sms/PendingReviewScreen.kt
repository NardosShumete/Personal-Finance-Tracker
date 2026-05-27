package com.portfolio.financetracker.ui.sms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Design tokens ─────────────────────────────────────────────────────────────
private val Slate900   = Color(0xFF0F172A)
private val GlassBg    = Color(0xFF1E293B).copy(alpha = 0.80f)
private val GlassBorder= Color.White.copy(alpha = 0.10f)
private val NeonGreen  = Color(0xFF10B981)
private val NeonRose   = Color(0xFFF43F5E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: PendingReviewViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val pending     by viewModel.pendingTransactions.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val snackbar    = remember { SnackbarHostState() }

    // Show snackbar when sync message arrives
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var showConfirmAllDialog by remember { mutableStateOf(false) }

    if (showConfirmAllDialog) {
        val totalIncome = pending.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = pending.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { showConfirmAllDialog = false },
            title = { Text("Confirm All Transactions") },
            text = {
                Text("${pending.size} transactions\n+ETB ${"%.2f".format(totalIncome)} income\n-ETB ${"%.2f".format(totalExpense)} expenses")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmAll()
                    showConfirmAllDialog = false
                }) {
                    Text("Confirm All", color = NeonGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAllDialog = false }) {
                    Text("Cancel", color = Color.White.copy(0.5f))
                }
            },
            containerColor = GlassBg,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(0.8f)
        )
    }

    // Edit sheet
    uiState.editingTransaction?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            onSave      = { amount, category, note, type ->
                viewModel.saveEdit(tx, amount, category, note, type)
            },
            onDismiss   = viewModel::dismissEdit
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.08f), Color.Transparent),
                        radius = 700f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost   = { SnackbarHost(snackbar) },
            topBar = {
                GlassTopBar(
                    title       = "Pending Review",
                    badge       = pending.size,
                    onBack      = onNavigateBack,
                    onSyncHistory = viewModel::syncHistory,
                    onConfirmAll  = { if (pending.isNotEmpty()) showConfirmAllDialog = true },
                    isSyncing   = uiState.isSyncing
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (syncProgress != null) {
                    LinearProgressIndicator(
                        progress = { syncProgress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonGreen,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
                
                if (pending.isEmpty()) {
                EmptyPendingState()
                } else {
                    val groupedPending = pending.groupBy { it.bankName ?: "Other" }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            GlassBanner(
                                text = "${pending.size} transaction${if (pending.size > 1) "s" else ""} " +
                                       "detected from bank SMS. Review before they affect your balance."
                            )
                        }

                        groupedPending.forEach { (bankName, transactions) ->
                            item {
                                Text(
                                    text = bankName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(transactions, key = { it.id }) { transaction ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn() + slideInVertically(
                                        initialOffsetY = { it / 2 },
                                        animationSpec  = spring(dampingRatio = 0.7f)
                                    )
                                ) {
                                    ApprovalCard(
                                        transaction = transaction,
                                        onConfirm   = { viewModel.confirm(transaction) },
                                        onEdit      = { viewModel.startEdit(transaction) },
                                        onDismiss   = { viewModel.dismiss(transaction) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

// ── Glass Top Bar ─────────────────────────────────────────────────────────────

@Composable
private fun GlassTopBar(
    title: String,
    badge: Int,
    onBack: () -> Unit,
    onSyncHistory: () -> Unit,
    onConfirmAll: () -> Unit,
    isSyncing: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900.copy(alpha = 0.92f))
            .border(0.5.dp, GlassBorder, RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Badge
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NeonRose),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Sync history button
            IconButton(onClick = onSyncHistory, enabled = !isSyncing) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NeonGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Sync, contentDescription = "Sync History", tint = NeonGreen)
                }
            }

            // Confirm all
            if (badge > 0) {
                TextButton(onClick = onConfirmAll) {
                    Text("All ✓", color = NeonGreen, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Floating Approval Card ────────────────────────────────────────────────────

@Composable
fun ApprovalCard(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isIncome   = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) NeonGreen else NeonRose
    val dateFormat  = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBg)
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.4f), GlassBorder)
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Subtle top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.2f))
                    )
                )
        )

        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // SMS source badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amount ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isIncome) "+" else "-",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ETB ${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Type label ────────────────────────────────────────────────────
            Text(
                text = if (isIncome) "CREDIT  ·  Income" else "DEBIT  ·  Expense",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Note + date ───────────────────────────────────────────────────
            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = dateFormat.format(Date(transaction.date)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )

            // ── Balance (if available) ────────────────────────────────────────
            transaction.smsBalance?.let { bal ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance after",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "ETB ${"%.2f".format(bal)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Raw SMS Expansion
            if (transaction.rawSms != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isExpanded) "Hide original SMS" else "Show original SMS", 
                        color = accentColor, 
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                AnimatedVisibility(visible = isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = transaction.rawSms,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dismiss
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                }

                // Edit
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }

                // Confirm — primary CTA
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Confirm",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Edit Transaction Sheet ────────────────────────────────────────────────────

@Composable
private fun EditTransactionSheet(
    transaction: Transaction,
    onSave: (Double, String, String, TransactionType) -> Unit,
    onDismiss: () -> Unit
) {
    var amount   by remember { mutableStateOf(transaction.amount.toString()) }
    var category by remember { mutableStateOf(transaction.category) }
    var note     by remember { mutableStateOf(transaction.note) }
    var type     by remember { mutableStateOf(transaction.type) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .border(0.5.dp, GlassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Type toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                        val selected = type == t
                        val color    = if (t == TransactionType.INCOME) NeonGreen else NeonRose
                        FilterChip(
                            selected = selected,
                            onClick  = { type = t },
                            label    = { Text(t.name) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor     = color
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(value = amount, label = "Amount (ETB)",
                    keyboardType = KeyboardType.Decimal) { amount = it }
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(value = category, label = "Category") { category = it }
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(value = note, label = "Note") { note = it }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.5f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull() ?: return@Button
                            onSave(parsedAmount, category, note, type)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Reusable glass text field ─────────────────────────────────────────────────

@Composable
private fun GlassTextField(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            focusedBorderColor      = NeonGreen,
            unfocusedBorderColor    = GlassBorder,
            cursorColor             = NeonGreen,
            focusedContainerColor   = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyPendingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "All caught up!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No pending transactions to review.\nNew bank SMS will appear here automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── Info banner ───────────────────────────────────────────────────────────────

@Composable
private fun GlassBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonGreen.copy(alpha = 0.08f))
            .border(0.5.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
