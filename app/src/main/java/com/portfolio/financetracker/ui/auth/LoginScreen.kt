package com.portfolio.financetracker.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.R

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val resetState by viewModel.resetState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHost = remember { SnackbarHostState() }

    // Navigation is handled by FinanceNavGraph via eventFlow.
    // This LaunchedEffect is kept as a safety net but onAuthSuccess is a no-op.
    LaunchedEffect(uiState.authResult) {
        if (uiState.authResult is com.portfolio.financetracker.domain.model.AuthResult.Success) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(resetState.isSuccess, resetState.errorMessage) {
        // Guard: don't fire on initial composition when both are default values
        if (!resetState.isSuccess && resetState.errorMessage == null) return@LaunchedEffect
        when {
            resetState.isSuccess -> {
                snackbarHost.showSnackbar(
                    message  = "Password reset email sent! Check your inbox.",
                    duration = SnackbarDuration.Long
                )
                viewModel.consumeResetState()
            }
            resetState.errorMessage != null -> {
                snackbarHost.showSnackbar(
                    message  = resetState.errorMessage!!,
                    duration = SnackbarDuration.Long
                )
                viewModel.consumeResetState()
            }
        }
    }

    // ── Local field state ─────────────────────────────────────────────────────
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var username        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            prefillEmail  = email,
            resetState    = resetState,
            onDismiss     = { if (!resetState.isLoading) showForgotDialog = false },
            onSend        = { resetEmail -> viewModel.sendPasswordReset(resetEmail) }
        )
    }

    // Close the dialog automatically when the reset succeeds or fails
    // (the snackbar LaunchedEffect above will show the result message)
    LaunchedEffect(resetState.isSuccess, resetState.errorMessage) {
        if (resetState.isSuccess || resetState.errorMessage != null) {
            showForgotDialog = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { scaffoldPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                // ── App icon ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.finance_tracker_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.isLoginMode) stringResource(R.string.sign_in_to_account)
                           else stringResource(R.string.create_new_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // ── Form card ─────────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        // ── Username (register only) ──────────────────────────
                        AnimatedVisibility(visible = !uiState.isLoginMode) {
                            Column {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = {
                                        username = it
                                        viewModel.onUsernameChanged(it)
                                    },
                                    label = { Text(stringResource(R.string.username)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    isError = uiState.usernameError != null,
                                    supportingText = uiState.usernameError?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // ── Email ─────────────────────────────────────────────
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                viewModel.onEmailChanged(it)
                            },
                            label = { Text(stringResource(R.string.email_address)) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            trailingIcon = {
                                // Green check when email is valid and non-empty
                                if (email.isNotEmpty() && uiState.emailError == null) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.valid_email),
                                        tint = Color(0xFF2ECC71)
                                    )
                                }
                            },
                            isError = uiState.emailError != null,
                            supportingText = uiState.emailError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Password ──────────────────────────────────────────
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                viewModel.onPasswordChanged(it, uiState.isLoginMode)
                            },
                            label = { Text(stringResource(R.string.password)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible)
                                            stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = uiState.passwordError != null,
                            supportingText = uiState.passwordError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (uiState.isLoginMode) viewModel.signIn(email, password)
                                    else viewModel.register(email, password, username)
                                }
                            )
                        )

                        // ── Password strength bar (register mode only) ────────
                        AnimatedVisibility(visible = !uiState.isLoginMode && password.isNotEmpty()) {
                            PasswordStrengthIndicator(password = password)
                        }

                        // ── Firebase-level error ──────────────────────────────
                        AnimatedVisibility(visible = uiState.errorMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // ── Forgot password (login mode only) ─────────────────────────
                AnimatedVisibility(visible = uiState.isLoginMode) {
                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Submit button — disabled until form is valid ───────────────
                Button(
                    onClick = {
                        if (uiState.isLoginMode) viewModel.signIn(email, password)
                        else viewModel.register(email, password, username)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    // Disabled while loading OR while form has validation errors
                    enabled = !uiState.isLoading && uiState.isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isLoginMode) stringResource(R.string.sign_in)
                                   else stringResource(R.string.create_account),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Toggle login / register ───────────────────────────────────
                TextButton(onClick = {
                    viewModel.toggleMode()
                    email = ""; password = ""; username = ""
                }) {
                    Text(
                        text = if (uiState.isLoginMode)
                            stringResource(R.string.dont_have_account_register)
                        else
                            stringResource(R.string.already_have_account_sign_in),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Password Strength Indicator ───────────────────────────────────────────────

@Composable
private fun PasswordStrengthIndicator(password: String) {
    // Score each rule independently
    val hasLength    = password.length >= 8
    val hasUpper     = password.any { it.isUpperCase() }
    val hasLower     = password.any { it.isLowerCase() }
    val hasDigit     = password.any { it.isDigit() }
    val hasSpecial   = password.any { it in "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/\\`~" }

    val score = listOf(hasLength, hasUpper, hasLower, hasDigit, hasSpecial).count { it }

    val (label, color) = when (score) {
        0, 1 -> stringResource(R.string.very_weak)  to Color(0xFFE74C3C)
        2    -> stringResource(R.string.weak)       to Color(0xFFE67E22)
        3    -> stringResource(R.string.fair)       to Color(0xFFF1C40F)
        4    -> stringResource(R.string.strong)     to Color(0xFF2ECC71)
        else -> stringResource(R.string.very_strong) to Color(0xFF27AE60)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        // Five segment bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < score) color
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Rule checklist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.strength_prefix, label),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Individual rule hints
        PasswordRuleRow(stringResource(R.string.password_rule_length), hasLength)
        PasswordRuleRow(stringResource(R.string.uppercase_letter), hasUpper)
        PasswordRuleRow(stringResource(R.string.lowercase_letter), hasLower)
        PasswordRuleRow(stringResource(R.string.number), hasDigit)
        PasswordRuleRow(stringResource(R.string.special_character), hasSpecial)
    }
}

@Composable
private fun PasswordRuleRow(label: String, passed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (passed) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = if (passed) Color(0xFF2ECC71)
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Forgot Password Dialog ────────────────────────────────────────────────────

@Composable
private fun ForgotPasswordDialog(
    prefillEmail: String,
    resetState: PasswordResetState,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(prefillEmail) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Validate email format in real-time as the user types
    fun validateAndSend() {
        val trimmed = resetEmail.trim()
        when {
            trimmed.isBlank() -> {
                emailError = "Please enter your email address."
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> {
                emailError = "Please enter a valid email address."
            }
            else -> {
                emailError = null
                onSend(trimmed)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!resetState.isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector        = Icons.Default.Email,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text       = stringResource(R.string.reset_password),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = stringResource(R.string.enter_email_reset_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value         = resetEmail,
                    onValueChange = {
                        resetEmail = it
                        // Clear error as soon as the user starts correcting
                        if (emailError != null) emailError = null
                    },
                    label        = { Text(stringResource(R.string.email_address)) },
                    leadingIcon  = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError      = emailError != null,
                    supportingText = emailError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine   = true,
                    enabled      = !resetState.isLoading,
                    modifier     = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { validateAndSend() }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { validateAndSend() },
                enabled  = !resetState.isLoading
            ) {
                if (resetState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.send_reset_email))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !resetState.isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
