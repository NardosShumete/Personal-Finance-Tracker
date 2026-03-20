package com.portfolio.financetracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.portfolio.financetracker.core.util.BiometricAuthenticator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.portfolio.financetracker.ui.auth.BiometricSetupScreen
import com.portfolio.financetracker.ui.auth.BiometricViewModel
import com.portfolio.financetracker.ui.navigation.FinanceNavGraph
import com.portfolio.financetracker.ui.theme.PersonalFinanceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import com.portfolio.financetracker.core.util.NotificationScheduler

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val biometricViewModel: BiometricViewModel by viewModels()
    private lateinit var authenticator: BiometricAuthenticator

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationScheduler.setupRecurringWork(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticator = BiometricAuthenticator(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationScheduler.setupRecurringWork(this)
        }

        setContent {
            val isDarkModeEnabled by biometricViewModel.isDarkModeEnabled.collectAsState()
            val useDarkTheme = isDarkModeEnabled ?: androidx.compose.foundation.isSystemInDarkTheme()
            val currencyCode by biometricViewModel.currencyCode.collectAsState()
            
            PersonalFinanceTrackerTheme(darkTheme = useDarkTheme) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.portfolio.financetracker.core.util.LocalCurrencyCode provides currencyCode
                ) {
                    val isOnboarded by biometricViewModel.isOnboarded.collectAsState()
                    val isFirstTime by biometricViewModel.isFirstTimeUser.collectAsState()
                    val isEnabled by biometricViewModel.isBiometricEnabled.collectAsState()
                    val isAuthenticated by biometricViewModel.isAuthenticated.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !isOnboarded -> {
                            com.portfolio.financetracker.ui.onboarding.OnboardingScreen(
                                onFinish = {
                                    biometricViewModel.completeOnboarding()
                                }
                            )
                        }
                        isFirstTime && authenticator.isBiometricAvailable() -> {
                            BiometricSetupScreen(
                                onSetupComplete = {
                                    // Set first time to false handles navigate to graph
                                }
                            )
                        }
                        isEnabled && !isAuthenticated -> {
                            // Show splash or locked screen while prompt is visible
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            
                            // Trigger prompt
                            LaunchedEffect(Unit) {
                                showBiometricPrompt()
                            }
                        }
                        else -> {
                            FinanceNavGraph()
                        }
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If app is returning from background and biometric is enabled, reset authentication
        if (biometricViewModel.isBiometricEnabled.value) {
            biometricViewModel.setAuthenticated(false)
        }
    }

    private fun showBiometricPrompt() {
        authenticator.promptBiometricAuth(
            activity = this,
            title = "Biometric Login",
            subtitle = "Authenticating for Personal Finance Tracker",
            onSuccess = {
                biometricViewModel.setAuthenticated(true)
            },
            onError = { error ->
                // Handle error (perhaps show a retry button or close app if critical)
            }
        )
    }
}
