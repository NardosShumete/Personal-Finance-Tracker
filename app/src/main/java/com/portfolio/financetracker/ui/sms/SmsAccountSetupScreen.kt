package com.portfolio.financetracker.ui.sms

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.core.sms.SmsInboxReader
import com.portfolio.financetracker.core.sms.SmsParser

// ── Design tokens (match PendingReviewScreen) ─────────────────────────────────
private val Slate900    = Color(0xFF0F172A)
private val GlassBg     = Color(0xFF1E293B).copy(alpha = 0.85f)
private val GlassBorder = Color.White.copy(alpha = 0.10f)
private val NeonGreen   = Color(0xFF10B981)
private val NeonRose    = Color(0xFFF43F5E)
private val NeonBlue    = Color(0xFF3B82F6)
private val NeonAmber   = Color(0xFFF59E0B)

@Composable
fun SmsAccountSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    viewModel: SmsAccountSetupViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val isSmsEnabled  by viewModel.isSmsEnabled.collectAsState()
    val trackedSenders by viewModel.trackedSenders.collectAsState()
    val snackbar      = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbar.showSnackbar("Saved! Historical SMS sync started in background.")
            onSetupComplete()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
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
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonBlue.copy(alpha = 0.08f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost   = { SnackbarHost(snackbar) },
            topBar = {
                SetupTopBar(
                    onBack          = onNavigateBack,
                    selectedCount   = uiState.selectedAddresses.size,
                    onSave          = viewModel::saveAndSync,
                    isSaving        = uiState.isSaving,
                    canSave         = uiState.selectedAddresses.isNotEmpty() || isSmsEnabled
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Explanation card ──────────────────────────────────────────
                item {
                    ExplanationCard()
                }

                // ── Current tracking status ───────────────────────────────────
                if (trackedSenders.isNotEmpty() && !uiState.hasScanned) {
                    item {
                        ActiveAccountsCard(
                            senders = trackedSenders,
                            onRescan = viewModel::scanInbox
                        )
                    }
                }

                // ── Scan button ───────────────────────────────────────────────
                item {
                    ScanButton(
                        isScanning = uiState.isScanning,
                        hasScanned = uiState.hasScanned,
                        onClick    = viewModel::scanInbox
                    )
                }

                // ── Results ───────────────────────────────────────────────────
                if (uiState.hasScanned) {
                    if (uiState.discoveredSenders.isEmpty()) {
                        item { NoSmsFoundCard() }
                    } else {
                        item {
                            // Select all / clear row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.discoveredSenders.size} bank account${if (uiState.discoveredSenders.size > 1) "s" else ""} found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = viewModel::selectAll) {
                                        Text("All", color = NeonGreen, style = MaterialTheme.typography.labelMedium)
                                    }
                                    TextButton(onClick = viewModel::clearAll) {
                                        Text("None", color = Color.White.copy(0.4f), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        items(uiState.discoveredSenders, key = { it.address }) { sender ->
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn() + slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec  = spring(dampingRatio = 0.75f)
                                )
                            ) {
                                SenderSelectionCard(
                                    sender     = sender,
                                    isSelected = sender.address in uiState.selectedAddresses,
                                    onToggle   = { viewModel.toggleSender(sender.address) }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun SetupTopBar(
    onBack: () -> Unit,
    selectedCount: Int,
    onSave: () -> Unit,
    isSaving: Boolean,
    canSave: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900.copy(alpha = 0.95f))
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bank Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (selectedCount > 0) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }
            if (canSave) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Sender selection card ─────────────────────────────────────────────────────

@Composable
private fun SenderSelectionCard(
    sender: SmsInboxReader.DiscoveredSender,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val accentColor = bankColor(sender.bankFormat)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                brush = if (isSelected)
                    Brush.linearGradient(listOf(accentColor, accentColor.copy(0.4f)))
                else
                    Brush.linearGradient(listOf(GlassBorder, GlassBorder)),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sender.bankFormat.emoji(),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sender.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // SMS preview
                Text(
                    text = "\"${sender.sampleBody}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Bank format badge
                    SmallBadge(
                        text  = sender.bankFormat.displayName(),
                        color = accentColor
                    )
                    // Message count badge
                    SmallBadge(
                        text  = "${sender.messageCount} SMS",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accentColor else Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun ExplanationCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NeonBlue.copy(alpha = 0.08f))
            .border(0.5.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null,
                    tint = NeonBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose Your Bank Accounts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan your SMS inbox to find bank notifications. " +
                       "Select only the accounts you want to track. " +
                       "Only those exact senders will be monitored — " +
                       "personal messages are never read.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ActiveAccountsCard(senders: Set<String>, onRescan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NeonGreen.copy(alpha = 0.08f))
            .border(0.5.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Currently tracking ${senders.size} account${if (senders.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen
                    )
                }
                TextButton(onClick = onRescan) {
                    Text("Change", color = NeonGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
            senders.forEach { sender ->
                Text(
                    text = "• $sender",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanButton(isScanning: Boolean, hasScanned: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isScanning,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hasScanned) GlassBg else NeonBlue
        )
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Scanning inbox…", color = Color.White)
        } else {
            Icon(
                if (hasScanned) Icons.Default.Refresh else Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (hasScanned) "Scan Again" else "Scan SMS Inbox",
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun NoSmsFoundCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No bank SMS found",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Make sure you have received bank transaction notifications in your SMS inbox.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun bankColor(format: SmsParser.BankFormat): Color = when (format) {
    SmsParser.BankFormat.CBE       -> Color(0xFF3B82F6)  // Blue
    SmsParser.BankFormat.DASHEN    -> Color(0xFF8B5CF6)  // Purple
    SmsParser.BankFormat.TELEBIRR  -> Color(0xFF10B981)  // Green
    SmsParser.BankFormat.AWASH     -> Color(0xFFF59E0B)  // Amber
    SmsParser.BankFormat.ABYSSINIA -> Color(0xFFF43F5E)  // Rose
    SmsParser.BankFormat.UNKNOWN   -> Color(0xFF94A3B8)  // Slate
}

private fun SmsParser.BankFormat.emoji(): String = when (this) {
    SmsParser.BankFormat.CBE       -> "🏦"
    SmsParser.BankFormat.DASHEN    -> "💜"
    SmsParser.BankFormat.TELEBIRR  -> "📱"
    SmsParser.BankFormat.AWASH     -> "🌊"
    SmsParser.BankFormat.ABYSSINIA -> "🏛️"
    SmsParser.BankFormat.UNKNOWN   -> "💳"
}

private fun SmsParser.BankFormat.displayName(): String = when (this) {
    SmsParser.BankFormat.CBE       -> "CBE"
    SmsParser.BankFormat.DASHEN    -> "Dashen"
    SmsParser.BankFormat.TELEBIRR  -> "Telebirr"
    SmsParser.BankFormat.AWASH     -> "Awash"
    SmsParser.BankFormat.ABYSSINIA -> "Abyssinia"
    SmsParser.BankFormat.UNKNOWN   -> "Bank"
}
