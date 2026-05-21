package com.portfolio.financetracker.ui.transaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.portfolio.financetracker.R
import com.portfolio.financetracker.domain.model.RecurringPeriod

/**
 * Extension function to provide localized strings for RecurringPeriod.
 * Separated from the domain model to resolve conflicts between Serialization and Compose compilers.
 */
@Composable
fun RecurringPeriod.toLocalizedString(): String {
    return when (this) {
        RecurringPeriod.NONE -> stringResource(R.string.recurring_period_once)
        RecurringPeriod.WEEKLY -> stringResource(R.string.recurring_period_weekly)
        RecurringPeriod.MONTHLY -> stringResource(R.string.recurring_period_monthly)
    }
}
