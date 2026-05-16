package com.portfolio.financetracker.core.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.portfolio.financetracker.core.worker.BudgetAlertWorker
import com.portfolio.financetracker.core.worker.WeeklyReportWorker
import com.portfolio.financetracker.core.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun setupRecurringWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Generate dynamic delay strictly for 6:00 PM Sunday
        val delayForWeeklyReport = calculateDelayToNextSunday1800()

        val weeklyReportRequest = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayForWeeklyReport, TimeUnit.MILLISECONDS)
            .build()
        
        // Enqueue Weekly Report Worker
        workManager.enqueueUniquePeriodicWork(
            "WeeklyFinanceReport",
            ExistingPeriodicWorkPolicy.UPDATE,
            weeklyReportRequest
        )

        // Queue Budget Monitor every 12 hours
        val budgetMonitorRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(12, TimeUnit.HOURS)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "BudgetMonitorAlert",
            ExistingPeriodicWorkPolicy.UPDATE,
            budgetMonitorRequest
        )

        // Queue Reminders Check every 1 hour
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "FinanceReminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    private fun calculateDelayToNextSunday1800(): Long {
        val currentCalendar = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 18) // 6 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCalendar.before(currentCalendar)) {
            // It's already past 6 PM Sunday this week, schedule for next week
            targetCalendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        
        return targetCalendar.timeInMillis - currentCalendar.timeInMillis
    }
}
