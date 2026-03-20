package com.portfolio.financetracker.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfolio.financetracker.core.util.NotificationHelper
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.use_case.TransactionUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import com.portfolio.financetracker.domain.use_case.GoalUseCases
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class WeeklyReportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionUseCases: TransactionUseCases,
    private val goalUseCases: GoalUseCases
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val currentMillis = System.currentTimeMillis()
            val oneWeekAgo = currentMillis - 604800000L

            val transactions = transactionUseCases.getTransactions().first()
            val weeklyTransactions = transactions.filter { it.date >= oneWeekAgo }
            
            val totalSpent = weeklyTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
                
            val topCategory = weeklyTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                ?.key ?: "None"

            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ET"))
            
            var message = "You spent ${formatter.format(totalSpent)} this week.\nTop Category: $topCategory"
            
            val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
            val goal = goalUseCases.getGoal(currentMonthYear).first()
            if (goal != null && goal.expenseLimit > 0) {
                // Calculate total expense for the WHOLE month
                val calendar = Calendar.getInstance()
                val currentMonthStart = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                
                val monthExpense = transactions
                    .filter { it.date >= currentMonthStart && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                
                val remaining = (goal.expenseLimit - monthExpense).coerceAtLeast(0.0)
                message += "\nRemaining Budget: ${formatter.format(remaining)}"
            }
            
            NotificationHelper.showNotification(
                context = context,
                title = "Weekly Financial Report",
                message = message,
                notificationId = 101
            )
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
