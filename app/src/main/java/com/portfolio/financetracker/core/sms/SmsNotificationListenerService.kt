package com.portfolio.financetracker.core.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.portfolio.financetracker.core.worker.SmsProcessWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Task 9 — Notification Listener Fallback
 *
 * For users who deny SMS permissions or use devices that restrict SMS reading,
 * we can intercept notifications from bank apps (like Telebirr or CBE app).
 * The notification text is routed through our existing parser pipeline.
 */
@AndroidEntryPoint
class SmsNotificationListenerService : NotificationListenerService() {

    // Injecting dependencies in a Service requires @AndroidEntryPoint
    // We will just enqueue the SmsProcessWorker to reuse the existing pipeline.

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Only process notifications from known bank apps or messaging apps
        if (!isBankApp(packageName) && !isMessagingApp(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val fullBody = "$title $text".trim()
        if (fullBody.isEmpty()) return

        val receivedAt = sbn.postTime
        val sender = title.takeIf { it.isNotBlank() } ?: packageName
        
        Log.d(TAG, "Intercepted notification from $packageName (sender=$sender)")

        // Reuse the exact same worker pipeline used by SMS.
        // We use applicationContext to avoid memory leaks.
        SmsProcessWorker.enqueue(
            context = applicationContext,
            sender = sender,
            body = fullBody,
            receivedAt = receivedAt
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed
    }

    private fun isBankApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("telebirr") ||
               lower.contains("cbe") ||
               lower.contains("dashen") ||
               lower.contains("awash") ||
               lower.contains("abyssinia") ||
               lower.contains("boa") ||
               lower.contains("bank")
    }

    private fun isMessagingApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        // Sometimes SMS comes through third-party SMS apps
        return lower.contains("mms") ||
               lower.contains("sms") ||
               lower.contains("messaging") ||
               lower.contains("telephony")
    }

    companion object {
        private const val TAG = "SmsNotifListener"
    }
}
