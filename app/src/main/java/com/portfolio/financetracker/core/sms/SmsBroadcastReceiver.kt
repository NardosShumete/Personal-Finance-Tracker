package com.portfolio.financetracker.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.portfolio.financetracker.core.worker.SmsProcessWorker

/**
 * Lightweight SMS BroadcastReceiver.
 *
 * Responsibilities (intentionally minimal):
 *  1. Verify the intent action is SMS_RECEIVED
 *  2. Reassemble multi-part messages
 *  3. Check sender against the allowlist (fast, no I/O)
 *  4. Hand off to [SmsProcessWorker] via WorkManager — all heavy work
 *     (parsing + DB insert) happens in the worker, not here
 *
 * This receiver does NOT use goAsync() or inject dependencies because
 * WorkManager.enqueue() is synchronous and completes in < 1 ms.
 *
 * Security:
 *  • android:exported="false" in manifest — only the system can trigger it
 *  • android:permission="BROADCAST_SMS" — only the telephony stack can send
 *  • Sender allowlist checked before any WorkManager enqueue
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Reassemble multi-part SMS grouped by sender
        messages.groupBy { it.originatingAddress ?: "" }
            .forEach { (sender, parts) ->
                val body = parts.joinToString("") { it.messageBody }

                // Fast body-based check — exits immediately for non-bank SMS
                // No I/O, no DB access, no WorkManager enqueue for personal messages
                if (SmsParser.detectBankFormat(body) == SmsParser.BankFormat.UNKNOWN) return@forEach

                val receivedAt = parts.first().timestampMillis

                // Delegate all parsing + DB work to WorkManager
                SmsProcessWorker.enqueue(context, sender, body, receivedAt)
                Log.d(TAG, "Enqueued SMS work for sender: $sender")
            }
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
}
