package com.portfolio.financetracker.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetGoalDialog(
    initialIncomeGoal: Double,
    initialExpenseLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var incomeGoal by remember { mutableStateOf(if (initialIncomeGoal > 0) initialIncomeGoal.toString() else "") }
    var expenseLimit by remember { mutableStateOf(if (initialExpenseLimit > 0) initialExpenseLimit.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set Monthly Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = incomeGoal,
                    onValueChange = { incomeGoal = it },
                    label = { Text("Income Goal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expenseLimit,
                    onValueChange = { expenseLimit = it },
                    label = { Text("Expense Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val income = incomeGoal.toDoubleOrNull() ?: 0.0
                    val expense = expenseLimit.toDoubleOrNull() ?: 0.0
                    onConfirm(income, expense)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
