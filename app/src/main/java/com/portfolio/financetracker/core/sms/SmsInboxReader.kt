package com.portfolio.financetracker.core.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads SMS from the device inbox.
 *
 * Two modes:
 * 1. [discoverBankSenders] — scans the inbox for messages that look like bank
 *    notifications and returns unique senders for the user to choose from.
 *    This is the "account setup" step.
 *
 * 2. [readFromTrackedSenders] — reads messages only from the exact sender
 *    addresses the user explicitly selected. Used for historical sync.
 */
object SmsInboxReader {

    private const val TAG = "SmsInboxReader"
    private val SMS_INBOX_URI: Uri = Uri.parse("content://sms/inbox")

    data class RawSms(
        val smsId: String,
        val sender: String,
        val body: String,
        val timestampMs: Long
    )

    /**
     * Represents a discovered bank sender the user can choose to track.
     */
    data class DiscoveredSender(
        val address: String,          // exact sender address (e.g. "+251911123456" or "CBE")
        val displayName: String,      // human-readable label
        val bankFormat: SmsParser.BankFormat,
        val sampleBody: String,       // first 120 chars of a real SMS for preview
        val messageCount: Int         // how many bank SMS from this sender
    )

    // ── Mode 1: Discovery ─────────────────────────────────────────────────────

    /**
     * Scans the SMS inbox for messages that contain bank-like keywords.
     * Returns unique senders grouped by address so the user can choose
     * which accounts to track.
     *
     * Does NOT filter by sender name — uses body content detection instead.
     * This is the fix for the "all CBE" bug.
     */
    suspend fun discoverBankSenders(context: Context): List<DiscoveredSender> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )

            // Build WHERE clause from body keywords — not sender names
            val bodyFilter = SmsParser.BANK_BODY_KEYWORDS
                .joinToString(" OR ") { "body LIKE '%$it%'" }

            val grouped = mutableMapOf<String, MutableList<RawSms>>()

            try {
                context.contentResolver.query(
                    SMS_INBOX_URI,
                    projection,
                    bodyFilter,
                    null,
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->
                    val idCol      = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyCol    = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateCol    = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    while (cursor.moveToNext()) {
                        val smsId  = cursor.getString(idCol)     ?: continue
                        val sender = cursor.getString(addressCol) ?: continue
                        val body   = cursor.getString(bodyCol)    ?: continue
                        val date   = cursor.getLong(dateCol)

                        // Verify body actually looks like a bank transaction
                        val format = SmsParser.detectBankFormat(body)
                        if (format == SmsParser.BankFormat.UNKNOWN) continue

                        grouped.getOrPut(sender) { mutableListOf() }
                            .add(RawSms(smsId, sender, body, date))
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "READ_SMS permission not granted", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning SMS inbox", e)
            }

            // Convert to DiscoveredSender list
            grouped.map { (address, messages) ->
                val sample = messages.first()
                val format = SmsParser.detectBankFormat(sample.body)
                DiscoveredSender(
                    address      = address,
                    displayName  = buildDisplayName(address, format),
                    bankFormat   = format,
                    sampleBody   = sample.body.take(120),
                    messageCount = messages.size
                )
            }.sortedByDescending { it.messageCount }
        }

    // ── Mode 2: Tracked sync ──────────────────────────────────────────────────

    /**
     * Reads up to [limit] messages from the exact [trackedSenders] addresses.
     * Used for historical backfill after the user has selected their accounts.
     */
    suspend fun readFromTrackedSenders(
        context: Context,
        trackedSenders: Set<String>,
        limit: Int = 200
    ): List<RawSms> = withContext(Dispatchers.IO) {
        if (trackedSenders.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<RawSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        // Query each tracked sender separately for precise matching
        for (sender in trackedSenders) {
            try {
                context.contentResolver.query(
                    SMS_INBOX_URI,
                    projection,
                    "address = ?",
                    arrayOf(sender),
                    "${Telephony.Sms.DATE} DESC LIMIT $limit"
                )?.use { cursor ->
                    val idCol   = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addrCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    while (cursor.moveToNext()) {
                        val smsId = cursor.getString(idCol)  ?: continue
                        val addr  = cursor.getString(addrCol) ?: continue
                        val body  = cursor.getString(bodyCol) ?: continue
                        val date  = cursor.getLong(dateCol)

                        // Only include messages that actually look like bank transactions
                        if (SmsParser.detectBankFormat(body) != SmsParser.BankFormat.UNKNOWN) {
                            results.add(RawSms(smsId, addr, body, date))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading SMS for sender $sender", e)
            }
        }

        Log.i(TAG, "Read ${results.size} bank SMS from ${trackedSenders.size} tracked senders")
        results
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildDisplayName(address: String, format: SmsParser.BankFormat): String {
        val bankLabel = when (format) {
            SmsParser.BankFormat.CBE       -> "CBE"
            SmsParser.BankFormat.DASHEN    -> "Dashen Bank"
            SmsParser.BankFormat.TELEBIRR  -> "Telebirr"
            SmsParser.BankFormat.AWASH     -> "Awash Bank"
            SmsParser.BankFormat.ABYSSINIA -> "Bank of Abyssinia"
            SmsParser.BankFormat.UNKNOWN   -> "Bank"
        }
        // If address looks like a phone number, show bank label + masked number
        return if (address.startsWith("+") || address.all { it.isDigit() || it == '+' }) {
            "$bankLabel (${address.takeLast(4).padStart(address.length, '*')})"
        } else {
            "$bankLabel ($address)"
        }
    }
}
