package com.portfolio.financetracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
//  Extended semantic finance colors
//  Access via: MaterialTheme.financeColors.income / .expense
// ─────────────────────────────────────────────────────────────────────────────

data class FinanceColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(
        income            = EmeraldGreen,
        onIncome          = Color.White,
        incomeContainer   = EmeraldBg,
        onIncomeContainer = EmeraldDark,
        expense           = ElectricRose,
        onExpense         = Color.White,
        expenseContainer  = RoseBg,
        onExpenseContainer= RoseDark,
    )
}

val MaterialTheme.financeColors: FinanceColors
    @Composable get() = LocalFinanceColors.current

// ─────────────────────────────────────────────────────────────────────────────
//  DARK color scheme — Midnight FinTech
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary              = EmeraldGreen,
    onPrimary            = Midnight,
    primaryContainer     = Color(0xFF064E3B),
    onPrimaryContainer   = EmeraldLight,

    secondary            = GradientBlue,
    onSecondary          = Midnight,
    secondaryContainer   = Color(0xFF1E3A5F),
    onSecondaryContainer = Slate200,

    tertiary             = GradientPurple,
    onTertiary           = Midnight,
    tertiaryContainer    = Color(0xFF2E1065),
    onTertiaryContainer  = Color(0xFFDDD6FE),

    error                = ElectricRose,
    onError              = Midnight,
    errorContainer       = Color(0xFF4C0519),
    onErrorContainer     = RoseLight,

    background           = Midnight,          // #0F172A
    onBackground         = SlateWhite,        // #F8FAFC
    surface              = MidnightSurface,   // #1E293B
    onSurface            = Slate200,          // #E2E8F0
    surfaceVariant       = Color(0xFF1E293B),
    onSurfaceVariant     = Slate400,          // #94A3B8
    outline              = Color(0xFF334155),
)

// ─────────────────────────────────────────────────────────────────────────────
//  LIGHT color scheme — Clean FinTech White
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = Color(0xFF059669),   // Darker emerald — readable on white
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFD1FAE5),   // Soft mint
    onPrimaryContainer   = Color(0xFF064E3B),

    secondary            = Color(0xFF2563EB),   // Blue
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A5F),

    tertiary             = Color(0xFF7C3AED),   // Purple
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFEDE9FE),
    onTertiaryContainer  = Color(0xFF2E1065),

    error                = Color(0xFFE11D48),   // Darker rose — readable on white
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFE4E6),
    onErrorContainer     = Color(0xFF9F1239),

    background           = Color(0xFFF8FAFC),   // Near-white
    onBackground         = Color(0xFF0F172A),   // Near-black
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1E293B),
    surfaceVariant       = Color(0xFFF1F5F9),   // Very light grey
    onSurfaceVariant     = Color(0xFF475569),
    outline              = Color(0xFFCBD5E1),
)

// ─────────────────────────────────────────────────────────────────────────────
//  Theme entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PersonalFinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val financeColors = if (darkTheme) {
        FinanceColors(
            income            = EmeraldGreen,
            onIncome          = Midnight,
            incomeContainer   = EmeraldBg,
            onIncomeContainer = EmeraldLight,
            expense           = ElectricRose,
            onExpense         = Midnight,
            expenseContainer  = RoseBg,
            onExpenseContainer= RoseLight,
        )
    } else {
        FinanceColors(
            income            = Color(0xFF059669),
            onIncome          = Color.White,
            incomeContainer   = Color(0xFFD1FAE5),
            onIncomeContainer = Color(0xFF064E3B),
            expense           = Color(0xFFE11D48),
            onExpense         = Color.White,
            expenseContainer  = Color(0xFFFFE4E6),
            onExpenseContainer= Color(0xFF9F1239),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = FinanceTypography,
            content     = content
        )
    }
}
