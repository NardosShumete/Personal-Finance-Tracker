package com.portfolio.financetracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Example composable demonstrating the new FinTech theme system.
 *
 * Best practices shown:
 * 1. Use MaterialTheme.financeColors.income / .expense for semantic colors
 * 2. Use MaterialTheme.typography with appropriate weights (Bold for balances, SemiBold for titles)
 * 3. Never hardcode colors — always reference theme tokens
 * 4. Use proper contrast ratios (onPrimary, onSurface, etc.)
 */
@Composable
fun FinanceBalanceCard(
    totalBalance: Double,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── Total Balance (Headline Bold) ──────────────────────────────
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${"%.2f".format(totalBalance)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Income / Expense Row ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+$${"%.2f".format(income)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.financeColors.income
                    )
                }

                // Expense
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-$${"%.2f".format(expense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.financeColors.expense
                    )
                }
            }
        }
    }
}

/**
 * Example transaction list item showing semantic color usage.
 */
@Composable
fun FinanceTransactionRow(
    category: String,
    amount: Double,
    isIncome: Boolean,
    date: String,
    modifier: Modifier = Modifier
) {
    val amountColor = if (isIncome) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense
    val amountPrefix = if (isIncome) "+" else "-"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$amountPrefix$${"%.2f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PreviewFinanceThemeLight() {
    PersonalFinanceTrackerTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinanceBalanceCard(
                totalBalance = 12450.75,
                income = 8500.00,
                expense = 3200.50
            )
            FinanceTransactionRow(
                category = "Salary",
                amount = 5000.00,
                isIncome = true,
                date = "May 1, 2026"
            )
            FinanceTransactionRow(
                category = "Groceries",
                amount = 120.50,
                isIncome = false,
                date = "May 3, 2026"
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun PreviewFinanceThemeDark() {
    PersonalFinanceTrackerTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinanceBalanceCard(
                totalBalance = 12450.75,
                income = 8500.00,
                expense = 3200.50
            )
            FinanceTransactionRow(
                category = "Salary",
                amount = 5000.00,
                isIncome = true,
                date = "May 1, 2026"
            )
            FinanceTransactionRow(
                category = "Groceries",
                amount = 120.50,
                isIncome = false,
                date = "May 3, 2026"
            )
        }
    }
}
