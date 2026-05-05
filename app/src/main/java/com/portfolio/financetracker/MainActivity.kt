package com.portfolio.financetracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.portfolio.financetracker.core.util.BiometricAuthenticator
import com.portfolio.financetracker.core.util.NotificationScheduler
import com.portfolio.financetracker.ui.auth.BiometricSetupScreen
import com.portfolio.financetracker.ui.auth.BiometricViewModel
import com.portfolio.financetracker.ui.auth.AuthViewModel
import com.portfolio.financetracker.ui.navigation.FinanceNavGraph
import com.portfolio.financetracker.ui.navigation.Screen
import com.portfolio.financetracker.ui.splash.SplashScreen
import com.portfolio.financetracker.ui.theme.PersonalFinanceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val biometricViewModel: BiometricViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var authenticator: BiometricAuthenticator

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) NotificationScheduler.setupRecurringWork(this)
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
            val useDarkTheme      = isDarkModeEnabled ?: isSystemInDarkTheme()
            val currencyCode      by biometricViewModel.currencyCode.collectAsState()
            val languageCode      by biometricViewModel.languageCode.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

            // Locale / config setup
            val context = androidx.compose.ui.platform.LocalContext.current
            val locale  = java.util.Locale(languageCode)
            java.util.Locale.setDefault(locale)
            val config  = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            val configContext    = context.createConfigurationContext(config)
            val localizedContext = object : android.content.ContextWrapper(context) {
                override fun getResources() = configContext.resources
                override fun getTheme()     = configContext.theme
            }

            PersonalFinanceTrackerTheme(darkTheme = useDarkTheme) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.portfolio.financetracker.core.util.LocalCurrencyCode provides currencyCode,
                    androidx.compose.ui.platform.LocalContext provides localizedContext,
                    androidx.compose.ui.platform.LocalConfiguration provides config
                ) {
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                        return@CompositionLocalProvider
                    }

                    val isOnboarded     by biometricViewModel.isOnboarded.collectAsState()
                    val isFirstTime     by biometricViewModel.isFirstTimeUser.collectAsState()
                    val isBioEnabled    by biometricViewModel.isBiometricEnabled.collectAsState()
                    val isAuthenticated by biometricViewModel.isAuthenticated.collectAsState()

                    // Determine start destination based on Firebase session
                    val startDestination = if (authViewModel.currentUser.collectAsState().value != null
                        || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
                    ) {
                        Screen.DashboardScreen.route
                    } else {
                        Screen.LoginScreen.route
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when {
                            // 1. Onboarding not done yet
                            !isOnboarded -> {
                                com.portfolio.financetracker.ui.onboarding.OnboardingScreen(
                                    onFinish = { biometricViewModel.completeOnboarding() }
                                )
                            }
                            // 2. First time — offer biometric setup
                            isFirstTime && authenticator.isBiometricAvailable() -> {
                                BiometricSetupScreen(onSetupComplete = {})
                            }
                            // 3. Biometric lock active — show prompt
                            isBioEnabled && !isAuthenticated -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                                LaunchedEffect(Unit) { showBiometricPrompt() }
                            }
                            // 4. All gates passed — show app with correct start screen
                            else -> {
                                FinanceNavGraph(
                                    startDestination = startDestination,
                                    authViewModel    = authViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (biometricViewModel.isBiometricEnabled.value) {
            biometricViewModel.setAuthenticated(false)
        }
    }

    private fun showBiometricPrompt() {
        authenticator.promptBiometricAuth(
            activity = this,
            title    = "Biometric Login",
            subtitle = "Authenticating for Personal Finance Tracker",
            onSuccess = { biometricViewModel.setAuthenticated(true) },
            onError   = { /* optionally show retry UI */ }
        )
    }
}
