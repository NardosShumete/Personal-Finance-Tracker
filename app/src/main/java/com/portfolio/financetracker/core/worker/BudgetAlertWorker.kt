package com.portfolio.financetracker.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfolio.financetracker.core.util.NotificationHelper
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionUseCases: TransactionUseCases,
    private val goalUseCases: GoalUseCases,
    private val dataStoreManager: DataStoreManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if user has enabled budget alerts
            val isEnabled = dataStoreManager.isBudgetAlertsEnabled.first()
            if (!isEnabled) return Result.success()

            val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
            val goal = goalUseCases.getGoal(currentMonthYear).first() ?: return Result.success() // No goals, skip
            
            if (goal.expenseLimit <= 0) return Result.success()

            val calendar = Calendar.getInstance()
            val currentMonthStart = calendar.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val transactions = transactionUseCases.getTransactions().first()
            val monthTransactions = transactions.filter { it.date >= currentMonthStart }
            
            val totalExpense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val progress = totalExpense / goal.expenseLimit
            
            if (progress >= 0.8) {
                // Determine alert level
                val percentString = (progress * 100).toInt().toString() + "%"
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ET"))
                val alertMsg = when {
                    progress >= 1.0 -> "DANGER: You have exceeded your monthly budget of ${formatter.format(goal.expenseLimit)}!"
                    progress >= 0.9 -> "WARNING: You used $percentString of your budget! Slow down!"
                    else -> "Alert: You have reached $percentString of your monthly budget."
                }
                
                NotificationHelper.showNotification(
                    context = context,
                    title = "Budget Threshold Alert",
                    message = alertMsg,
                    notificationId = 102
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
