package com.portfolio.financetracker.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.portfolio.financetracker.core.worker.SmsProcessWorker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { sms ->
                val sender = sms.originatingAddress ?: return@forEach
                val body = sms.messageBody ?: return@forEach
                val receivedAt = sms.timestampMillis
                
                // Enqueue work to process SMS
                SmsProcessWorker.enqueue(context, sender, body, receivedAt)
            }
        }
    }
}
