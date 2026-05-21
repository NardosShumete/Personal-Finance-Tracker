package com.portfolio.financetracker.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.ui.auth.AuthViewModel

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userProfile by viewModel.currentUser.collectAsState()

    LaunchedEffect(userProfile) {
        // currentUser flow only emits a non-null profile when the user is
        // signed in AND email-verified (AuthRepositoryImpl filters unverified users).
        if (userProfile != null) {
            onNavigateToDashboard()
            return@LaunchedEffect
        }

        // No verified profile in the flow. Check if there is a Firebase session
        // at all (signed in but unverified) — only make the network call when
        // a Firebase user actually exists to avoid unnecessary reloads.
        if (viewModel.hasUnverifiedSession()) {
            onNavigateToVerifyEmail()
        } else {
            onNavigateToLogin()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
