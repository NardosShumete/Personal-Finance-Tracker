package com.portfolio.financetracker.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.portfolio.financetracker.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntOfExtra("REMINDER_ID", -1)
        
        if (intent.action == "ACTION_COMPLETE" && reminderId != -1) {
            // Cancel the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(reminderId)

            // Mark as completed in DB
            CoroutineScope(Dispatchers.IO).launch {
                repository.updateCompletionStatus(reminderId, true)
            }
        }
    }

    private fun Intent.getIntOfExtra(name: String, defaultValue: Int): Int {
        return if (hasExtra(name)) getIntExtra(name, defaultValue) else defaultValue
    }
}
