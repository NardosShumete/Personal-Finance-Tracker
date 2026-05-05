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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.portfolio.financetracker.core.util.BiometricAuthenticator
import androidx.compose.runtime.collectAsState
import com.portfolio.financetracker.ui.auth.BiometricSetupScreen
import com.portfolio.financetracker.ui.auth.BiometricViewModel
import com.portfolio.financetracker.ui.navigation.FinanceNavGraph
import com.portfolio.financetracker.ui.splash.SplashScreen
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
            val languageCode by biometricViewModel.languageCode.collectAsState()

            // Splash state — starts true, flipped to false when animation finishes
            var showSplash by remember { mutableStateOf(true) }

            val context = androidx.compose.ui.platform.LocalContext.current
            val locale = java.util.Locale(languageCode)
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            val configContext = context.createConfigurationContext(config)
            val localizedContext = object : android.content.ContextWrapper(context) {
                override fun getResources() = configContext.resources
                override fun getTheme() = configContext.theme
            }

            PersonalFinanceTrackerTheme(darkTheme = useDarkTheme) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.portfolio.financetracker.core.util.LocalCurrencyCode provides currencyCode,
                    androidx.compose.ui.platform.LocalContext provides localizedContext,
                    androidx.compose.ui.platform.LocalConfiguration provides config
                ) {
                    if (showSplash) {
                        // Show animated splash; dismiss when animation completes (~1200 ms)
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
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
                                    // Show locked screen while biometric prompt is visible
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }

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
