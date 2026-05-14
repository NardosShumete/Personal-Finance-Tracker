package com.portfolio.financetracker.core.sms

/**
 * Task 5 — Sealed result type for the parser pipeline.
 *
 * WHY a sealed class instead of nullable:
 * • Nullable return forces callers to guess WHY parsing failed.
 * • ParseResult.Failure carries a human-readable reason and the raw body,
 *   enabling structured logging and future analytics on parse failures.
 * • ParseResult.Ignored is distinct from Failure — it means "this is not
 *   a bank SMS" (expected), vs Failure which means "this looked like a
 *   bank SMS but we couldn't extract data" (unexpected, worth logging).
 */
sealed class ParseResult {
    /** Successfully parsed — contains the structured transaction data. */
    data class Success(val parsed: SmsParser.ParsedSms) : ParseResult()

    /**
     * Not a bank SMS — sender not tracked or body doesn't match any pattern.
     * This is the normal case for personal messages. Not logged as an error.
     */
    object Ignored : ParseResult()

    /**
     * Looked like a bank SMS but parsing failed.
     * Logged at WARN level for monitoring. Never crashes the app.
     */
    data class Failure(
        val reason: String,
        val rawBody: String,
        val bankFormat: SmsParser.BankFormat = SmsParser.BankFormat.UNKNOWN
    ) : ParseResult()
}
