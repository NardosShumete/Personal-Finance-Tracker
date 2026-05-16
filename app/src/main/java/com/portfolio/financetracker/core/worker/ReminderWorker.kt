package com.portfolio.financetracker.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.portfolio.financetracker.core.receiver.ReminderActionReceiver
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
import com.portfolio.financetracker.domain.model.TransactionType
import com.portfolio.financetracker.domain.repository.ReminderRepository
import com.portfolio.financetracker.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ReminderRepository,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val pendingReminders = repository.getPendingReminders(currentTime)

        pendingReminders.forEach { reminder ->
            // 1. Auto-generate expense if enabled
            if (reminder.autoGenerateExpense && !reminder.isCompleted) {
                val transaction = Transaction(
                    amount = reminder.amount,
                    category = reminder.category,
                    date = currentTime,
                    type = if (reminder.type == "DEPOSIT") TransactionType.INCOME else TransactionType.EXPENSE,
                    note = "Auto-generated from reminder: ${reminder.title}",
                    source = TransactionSource.MANUAL
                )
                transactionRepository.insertTransaction(transaction)
            }

            // 2. Send Notification with Sound and Actions
            sendNotification(reminder.id, reminder.title, "Due: ${reminder.amount} (${reminder.type})")

            // 3. Mark as completed
            repository.updateCompletionStatus(reminder.id, true)
            
            // 4. Handle Recurring Reminders (Monthly/Weekly)
            if (reminder.repeatInterval != "NONE") {
                val nextDate = Calendar.getInstance().apply {
                    timeInMillis = reminder.date
                    when (reminder.repeatInterval) {
                        "WEEKLY" -> add(Calendar.WEEK_OF_YEAR, 1)
                        "MONTHLY" -> add(Calendar.MONTH, 1)
                    }
                }.timeInMillis
                
                repository.insertReminder(reminder.copy(id = 0, date = nextDate, isCompleted = false))
            }
            
            // 5. Google Calendar Sync (Placeholder)
            if (reminder.syncToGoogleCalendar) {
                // TODO: Integrate with Google Calendar Provider API
            }
        }

        return Result.success()
    }

    private fun sendNotification(reminderId: Int, title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "finance_reminders_high"

        val channel = NotificationChannel(channelId, "Finance Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Urgent finance reminders with sound"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val completeIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("REMINDER_ID", reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            applicationContext, reminderId, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_save, "Mark as Paid", completePendingIntent)
            .build()

        notificationManager.notify(reminderId, notification)
    }
}
