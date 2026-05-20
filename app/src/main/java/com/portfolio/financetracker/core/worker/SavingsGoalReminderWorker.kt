package com.portfolio.financetracker.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfolio.financetracker.core.util.NotificationHelper
import com.portfolio.financetracker.domain.model.SavingsGoalStatus
import com.portfolio.financetracker.domain.repository.SavingsGoalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SavingsGoalReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SavingsGoalRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val goals = repository.getAllGoals().first()
        val currentTime = System.currentTimeMillis()

        goals.forEachIndexed { index, goal ->
            if (goal.status == SavingsGoalStatus.ACTIVE) {
                val timeRemaining = goal.deadlineDate - currentTime
                
                // Remind if deadline is within 3 days
                if (timeRemaining in 0..TimeUnit.DAYS.toMillis(3)) {
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Goal Deadline Approaching",
                        "Your goal '${goal.title}' is ending soon! You need ${goal.remainingAmount} more.",
                        notificationId = index + 1000
                    )
                }
                
                // Achievement: Reached 100%
                if (goal.progress >= 1f) {
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Goal Completed! \uD83C\uDF89",
                        "Congratulations! You've reached your target for '${goal.title}'.",
                        notificationId = index + 2000
                    )
                }
            }
        }

        return Result.success()
    }
}
