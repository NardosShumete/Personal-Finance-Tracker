package com.portfolio.financetracker.core.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Task 4 — Timestamp parsing utility.
 *
 * WHY out-of-order SMS matters:
 * Delayed SMS (hours or days late) arrive with the DEVICE receive time,
 * not the actual transaction time. If we use receive time, a transaction
 * that happened Monday appears as if it happened Thursday — corrupting
 * the user's financial timeline.
 *
 * Strategy:
 * 1. Try to extract a date from the SMS body (most accurate).
 * 2. If the body has no date, fall back to the SMS receive timestamp.
 * 3. If the parsed date is in the future (clock skew), fall back to receive time.
 * 4. All parsing is wrapped in try-catch — never crashes.
 *
 * Timezone:
 * Ethiopian banks send times in EAT (UTC+3). We parse in EAT and convert
 * to UTC milliseconds for storage. The UI formats using the device locale.
 */
object SmsTimestampParser {

    private const val TAG = "SmsTimestampParser"
    private val EAT = TimeZone.getTimeZone("Africa/Addis_Ababa")  // UTC+3

    // Ordered by specificity — most specific (with time) tried first
    private val DATE_FORMATS = listOf(
        // With time
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd/MM/yyyy HH:mm",    Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd-MM-yyyy HH:mm",    Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("yyyy-MM-dd HH:mm",    Locale.ENGLISH).apply { timeZone = EAT },
        // Date only — time defaults to 00:00:00 EAT
        SimpleDateFormat("dd/MM/yyyy",          Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd-MM-yyyy",          Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd-MMM-yyyy",         Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("yyyy-MM-dd",          Locale.ENGLISH).apply { timeZone = EAT },
        SimpleDateFormat("dd MMM yyyy",         Locale.ENGLISH).apply { timeZone = EAT }
    )

    // Regex to find date-like strings in SMS body
    private val DATE_CANDIDATES = listOf(
        Regex("""\b(\d{2}[/\-]\d{2}[/\-]\d{4}\s+\d{2}:\d{2}(?::\d{2})?)\b"""),
        Regex("""\b(\d{4}[/\-]\d{2}[/\-]\d{2}\s+\d{2}:\d{2}(?::\d{2})?)\b"""),
        Regex("""\b(\d{2}[/\-]\d{2}[/\-]\d{4})\b"""),
        Regex("""\b(\d{2}[/\-][A-Za-z]{3}[/\-]\d{4})\b"""),
        Regex("""\b(\d{4}[/\-]\d{2}[/\-]\d{2})\b"""),
        Regex("""\b(\d{2}\s+[A-Za-z]{3}\s+\d{4})\b""")
    )

    /**
     * Extracts the transaction timestamp from [body].
     * Falls back to [receivedAtMs] if no date found or parsing fails.
     *
     * @param body        The full SMS body text
     * @param receivedAtMs The time the SMS was received (from SmsMessage.timestampMillis)
     * @return UTC milliseconds representing the transaction time
     */
    fun extractOrFallback(body: String, receivedAtMs: Long): Long {
        val candidate = findDateCandidate(body) ?: return receivedAtMs

        for (format in DATE_FORMATS) {
            try {
                val parsed = format.parse(candidate) ?: continue
                val parsedMs = parsed.time

                // Sanity check: reject dates more than 1 day in the future
                // (clock skew protection) or more than 5 years in the past
                val now = System.currentTimeMillis()
                val fiveYearsMs = 5L * 365 * 24 * 60 * 60 * 1000
                if (parsedMs > now + 86_400_000L || parsedMs < now - fiveYearsMs) {
                    Log.w(TAG, "Parsed date $parsedMs out of reasonable range, using receivedAt")
                    continue
                }

                Log.d(TAG, "Extracted timestamp from body: $candidate → $parsedMs")
                return parsedMs
            } catch (e: Exception) {
                // Try next format
            }
        }

        Log.d(TAG, "Could not parse date candidate '$candidate', falling back to receivedAt")
        return receivedAtMs
    }

    private fun findDateCandidate(body: String): String? =
        DATE_CANDIDATES.firstNotNullOfOrNull { regex ->
            regex.find(body)?.groupValues?.get(1)
        }
}
