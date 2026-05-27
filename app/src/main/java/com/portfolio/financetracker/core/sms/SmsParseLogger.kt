package com.portfolio.financetracker.core.sms

import android.util.Log

/**
 * Structured logger for the SMS parsing pipeline.
 *
 * Centralises all parse-related log output so it can be:
 * - Filtered in Logcat with tag "SmsParseLogger"
 * - Easily disabled in release builds by changing LOG_ENABLED
 * - Extended to write to a local file or crash-reporting service
 *
 * Log levels:
 *   d() — expected non-events (non-bank SMS, duplicates)
 *   i() — successful parses
 *   w() — parse failures on bank-format SMS (unexpected, worth investigating)
 *   e() — crashes / exceptions
 */
object SmsParseLogger {

    private const val TAG = "SmsParseLogger"

    // Set to false in release builds to silence debug noise
    private const val LOG_ENABLED = true

    fun d(message: String) {
        if (LOG_ENABLED) Log.d(TAG, message)
    }

    fun i(message: String) {
        if (LOG_ENABLED) Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (LOG_ENABLED) {
            if (throwable != null) Log.w(TAG, message, throwable)
            else Log.w(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        // Always log errors regardless of LOG_ENABLED
        if (throwable != null) Log.e(TAG, message, throwable)
        else Log.e(TAG, message)
    }

    /** Logs a successful parse with key fields for easy debugging. */
    fun logSuccess(bank: String, type: String, amount: Double, balance: Double?, sender: String) {
        i("✓ PARSED [$bank] type=$type amount=${"%.2f".format(amount)}" +
          (if (balance != null) " balance=${"%.2f".format(balance)}" else "") +
          " sender=$sender")
    }

    /** Logs a parse failure with the raw body for diagnosis. */
    fun logFailure(bank: String, reason: String, rawBody: String) {
        w("✗ FAILED [$bank] reason=$reason body=${rawBody.take(80).replace("\n", " ")}")
    }

    /** Logs a duplicate skip. */
    fun logDuplicate(hash: String, sender: String) {
        d("⊘ DUPLICATE hash=${hash.take(12)}… sender=$sender")
    }

    /** Logs a non-bank SMS being ignored. */
    fun logIgnored(sender: String, reason: String) {
        d("— IGNORED sender=$sender reason=$reason")
    }
}
