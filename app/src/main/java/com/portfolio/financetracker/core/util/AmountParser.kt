package com.portfolio.financetracker.core.util

import android.util.Log

/**
 * Task 1 — Safe amount parsing utility.
 *
 * WHY this lives here (not in SmsParser or Repository):
 * • SmsParser is responsible for pattern matching — not number sanitization.
 * • Repository is responsible for persistence — not business rules.
 * • This utility is pure Kotlin with no Android/Room dependencies,
 *   making it trivially unit-testable and reusable across the codebase.
 *
 * Rules enforced:
 * 1. Amounts stored in Room are ALWAYS positive — TransactionType carries sign.
 * 2. Commas and whitespace are stripped before parsing.
 * 3. Negative signs are stripped (the type already encodes debit/credit).
 * 4. Zero amounts are rejected — a zero-value transaction is meaningless.
 * 5. Unrealistically large amounts (> 10 million ETB) are rejected as malformed.
 * 6. NumberFormatException is caught and logged — never crashes the parser.
 *
 * IMPORTANT — what we do NOT strip:
 * • We do NOT strip letters from the raw string with a blanket [a-z]+ regex.
 *   That approach was causing the "33" bug: if the captured group contained
 *   any trailing text (e.g. "1,500.00.Available"), stripping letters left a
 *   trailing dot that made toDoubleOrNull() return null, causing the parser
 *   to fall through to the generic fallback which then picked up the wrong
 *   number (e.g. the account number "33" or the balance instead of the amount).
 *
 * Instead we:
 * 1. Extract only the leading numeric portion (digits, commas, one decimal point).
 * 2. Strip a known currency prefix (ETB / Birr) if present at the start.
 * 3. Never touch anything after the last digit.
 */
object AmountParser {

    private const val TAG = "AmountParser"
    private const val MAX_REASONABLE_AMOUNT = 10_000_000.0  // 10 million ETB ceiling

    /**
     * Sealed result type — forces callers to handle both cases explicitly.
     */
    sealed class AmountResult {
        data class Success(val amount: Double) : AmountResult()
        data class Failure(val reason: String) : AmountResult()
    }

    /**
     * Parses a raw amount string extracted from an SMS body.
     *
     * Handles:
     *   "1,500.00"       → 1500.0   ✓
     *   "-500"           → 500.0    ✓ (negative sign stripped; type carries sign)
     *   " 3000 "         → 3000.0   ✓ (whitespace trimmed)
     *   "ETB 200"        → 200.0    ✓ (known currency prefix stripped)
     *   "1,500.00.Next"  → 1500.0   ✓ (trailing non-numeric chars ignored)
     *   "0.00"           → Failure  ✗ (zero rejected)
     *   ""               → Failure  ✗ (empty rejected)
     *   "abc"            → Failure  ✗ (non-numeric rejected)
     *   "99999999"       → Failure  ✗ (unreasonably large rejected)
     */
    fun parse(raw: String): AmountResult {
        if (raw.isBlank()) return AmountResult.Failure("Empty amount string")

        var sanitized = raw.trim()

        // Strip a leading currency prefix only (ETB, Birr, USD, etc.)
        // We only strip from the START to avoid touching digits in the middle.
        sanitized = sanitized.replace(Regex("^(?i)(etb|birr|usd|eur|gbp)\\s*"), "")

        // Strip leading negative sign (TransactionType carries direction)
        sanitized = sanitized.removePrefix("-").trim()

        if (sanitized.isEmpty()) return AmountResult.Failure("Amount is empty after prefix strip: '$raw'")

        // Extract only the leading numeric portion: digits, commas, and at most one decimal point.
        // This safely ignores any trailing text (e.g. "1,500.00.Available" → "1,500.00").
        val numericMatch = Regex("""^([\d,]+(?:\.\d+)?)""").find(sanitized)
            ?: return AmountResult.Failure("No numeric content found in '$sanitized' (raw: '$raw')")

        val numericStr = numericMatch.groupValues[1].replace(",", "")

        val value = numericStr.toDoubleOrNull()
            ?: return AmountResult.Failure("Cannot parse '$numericStr' as a number (raw: '$raw')")
                .also { Log.w(TAG, "Amount parse failure: raw='$raw' numeric='$numericStr'") }

        if (value <= 0.0) return AmountResult.Failure("Amount must be positive, got $value (raw: '$raw')")

        if (value > MAX_REASONABLE_AMOUNT)
            return AmountResult.Failure("Amount $value exceeds ceiling $MAX_REASONABLE_AMOUNT (raw: '$raw')")
                .also { Log.w(TAG, "Suspiciously large amount: $value") }

        return AmountResult.Success(value)
    }

    /**
     * Convenience extension — returns null on failure (for use in parsers
     * that already return null to signal "not a bank SMS").
     */
    fun String.toSafeAmount(): Double? = when (val result = parse(this)) {
        is AmountResult.Success -> result.amount
        is AmountResult.Failure -> {
            Log.d(TAG, result.reason)
            null
        }
    }
}
