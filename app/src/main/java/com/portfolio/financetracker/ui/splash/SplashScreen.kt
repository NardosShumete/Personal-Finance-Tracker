package com.portfolio.financetracker.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash screen shown on app launch.
 *
 * Animation sequence:
 *  1. Icon scales from 0.3 → 1.1 (overshoot) → 1.0 with a bounce feel  (0–800 ms)
 *  2. App name fades in simultaneously                                    (0–600 ms)
 *  3. After a short hold the whole screen fades out                       (900–1100 ms)
 *  4. [onSplashFinished] is called so MainActivity can proceed            (~1200 ms)
 *
 * Total visible duration: ~1200 ms — well within the 500–1500 ms requirement.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // --- animatable values ---
    val iconScale   = remember { Animatable(0.3f) }
    val iconAlpha   = remember { Animatable(0f)   }
    val textAlpha   = remember { Animatable(0f)   }
    val screenAlpha = remember { Animatable(1f)   }

    LaunchedEffect(Unit) {
        // 1. Scale + fade-in the icon with a slight overshoot (bounce)
        launch {
            iconScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            iconScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
        }
        launch {
            iconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }

        // 2. Fade-in the app name slightly after the icon starts
        delay(150)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }

        // 3. Hold briefly, then fade the whole screen out
        delay(700)
        screenAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )

        // 4. Notify caller to proceed
        onSplashFinished()
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Animated icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name fades in after icon
            Text(
                text = "Finance Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "by Group 2",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
