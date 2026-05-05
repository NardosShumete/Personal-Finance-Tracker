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

// ─────────────────────────────────────────────
//  Extended colors — semantic finance tokens
//  Access via: MaterialTheme.financeColors.income / .expense
// ─────────────────────────────────────────────

data class FinanceColors(
    /** Green — income, positive balance, success states */
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    /** Red — expense, negative balance, warning states */
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(
        income           = IncomeGreen,
        onIncome         = IncomeGreenOn,
        incomeContainer  = GreenLight,
        onIncomeContainer= GreenDark,
        expense          = ExpenseRed,
        onExpense        = ExpenseRedOn,
        expenseContainer = RedLight,
        onExpenseContainer = RedDark,
    )
}

/** Convenience extension — use `MaterialTheme.financeColors` anywhere in the UI */
val MaterialTheme.financeColors: FinanceColors
    @Composable get() = LocalFinanceColors.current

// ─────────────────────────────────────────────
//  Material 3 color schemes
// ─────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = Navy900,
    onPrimary        = NavyOnLight,
    primaryContainer = Navy200,
    onPrimaryContainer = Navy800,

    secondary        = SteelBlue,
    onSecondary      = SteelBlueOn,
    secondaryContainer = SteelBlue100,
    onSecondaryContainer = SteelBlue900,

    // Tertiary maps to "income green" so existing code using .tertiary gets green
    tertiary         = IncomeGreen,
    onTertiary       = IncomeGreenOn,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = GreenDark,

    error            = ExpenseRed,
    onError          = ExpenseRedOn,
    errorContainer   = RedLight,
    onErrorContainer = RedDark,

    background       = OffWhite,
    onBackground     = DarkText,
    surface          = White,
    onSurface        = DarkText,
    surfaceVariant   = LightGrey,
    onSurfaceVariant = MidGrey,
    outline          = MidGrey,
)

private val DarkColorScheme = darkColorScheme(
    primary          = DarkNavyOn,       // light lavender-blue on dark bg
    onPrimary        = DarkNavyCont,
    primaryContainer = DarkNavyCont,
    onPrimaryContainer = DarkNavyContOn,

    secondary        = DarkSteel,
    onSecondary      = DarkSteelOn,
    secondaryContainer = DarkSteelCont,
    onSecondaryContainer = DarkSteelContOn,

    tertiary         = DarkGreen,
    onTertiary       = DarkGreenOn,
    tertiaryContainer = DarkGreenCont,
    onTertiaryContainer = DarkGreenContOn,

    error            = DarkRed,
    onError          = DarkRedOn,
    errorContainer   = DarkRedCont,
    onErrorContainer = DarkRedContOn,

    background       = DarkBg,
    onBackground     = DarkOnBg,
    surface          = DarkSurface,
    onSurface        = DarkOnSurface,
    surfaceVariant   = DarkSurfaceVar,
    onSurfaceVariant = DarkOnSurface,
    outline          = DarkOutline,
)

// ─────────────────────────────────────────────
//  Theme entry point
// ─────────────────────────────────────────────

@Composable
fun PersonalFinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — we own the brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val financeColors = if (darkTheme) {
        FinanceColors(
            income            = DarkGreen,
            onIncome          = DarkGreenOn,
            incomeContainer   = DarkGreenCont,
            onIncomeContainer = DarkGreenContOn,
            expense           = DarkRed,
            onExpense         = DarkRedOn,
            expenseContainer  = DarkRedCont,
            onExpenseContainer= DarkRedContOn,
        )
    } else {
        FinanceColors(
            income            = IncomeGreen,
            onIncome          = IncomeGreenOn,
            incomeContainer   = GreenLight,
            onIncomeContainer = GreenDark,
            expense           = ExpenseRed,
            onExpense         = ExpenseRedOn,
            expenseContainer  = RedLight,
            onExpenseContainer= RedDark,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status bar — let the scaffold background show through
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
