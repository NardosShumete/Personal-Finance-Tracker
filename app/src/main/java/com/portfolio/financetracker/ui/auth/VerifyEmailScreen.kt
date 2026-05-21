package com.portfolio.financetracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState  = remember { SnackbarHostState() }

    // Collect one-shot events (snackbar messages) from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is AuthViewModel.UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(
                    message  = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Poll Firebase every 3 seconds to detect when the user clicks the link
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000)
            if (viewModel.checkEmailVerification()) {
                onVerified()
                break
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Email,
                contentDescription = null,
                modifier           = Modifier.size(100.dp),
                tint               = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text       = "Verify your email",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text      = "We've sent a verification link to your email address. " +
                            "Please click the link to verify your account.",
                style     = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick  = { viewModel.resendVerificationEmail() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resend Verification Email")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    viewModel.signOut()
                    onLogout()
                }
            ) {
                Text("Cancel and Sign Out")
            }
        }
    }
}
