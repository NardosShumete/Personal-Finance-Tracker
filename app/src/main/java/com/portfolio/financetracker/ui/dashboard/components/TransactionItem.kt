package com.portfolio.financetracker.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.ui.theme.financeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.portfolio.financetracker.R

@Composable
fun getCategoryStringOrFallback(key: String): String {
    return when(key.lowercase()) {
        "food", "cat_food" -> stringResource(R.string.cat_food)
        "transport", "cat_transport" -> stringResource(R.string.cat_transport)
        "shopping", "cat_shopping" -> stringResource(R.string.cat_shopping)
        "housing", "cat_housing" -> stringResource(R.string.cat_housing)
        "utilities", "cat_utilities" -> stringResource(R.string.cat_utilities)
        "salary", "cat_salary" -> stringResource(R.string.cat_salary)
        "freelance", "cat_freelance" -> stringResource(R.string.cat_freelance)
        "investment", "cat_investment" -> stringResource(R.string.cat_investment)
        "other", "cat_other" -> stringResource(R.string.cat_other)
        else -> key
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyCode = LocalCurrencyCode.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense
    val amountPrefix = if (isIncome) "+" else "-"

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCategoryStringOrFallback(transaction.category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val formattedAmount = CurrencyHelper.formatAmount(transaction.amount, currencyCode)
                Text(
                    text = "$amountPrefix$formattedAmount",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
