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
 * 2. Commas, spaces, and currency symbols are stripped before parsing.
 * 3. Negative signs are stripped (the type already encodes debit/credit).
 * 4. Zero amounts are rejected — a zero-value transaction is meaningless.
 * 5. Unrealistically large amounts (> 10 million ETB) are rejected as malformed.
 * 6. NumberFormatException is caught and logged — never crashes the parser.
 */
object AmountParser {

    private const val TAG = "AmountParser"
    private const val MAX_REASONABLE_AMOUNT = 10_000_000.0  // 10 million ETB ceiling

    /**
     * Sealed result type — forces callers to handle both cases explicitly.
     * Using a sealed class instead of nullable Double makes the failure
     * reason visible at the call site.
     */
    sealed class AmountResult {
        data class Success(val amount: Double) : AmountResult()
        data class Failure(val reason: String) : AmountResult()
    }

    /**
     * Parses a raw amount string extracted from an SMS body.
     *
     * Handles:
     *   "1,500.00"   → 1500.0   ✓
     *   "-500"       → 500.0    ✓ (negative sign stripped; type carries sign)
     *   " 3000 "     → 3000.0   ✓ (whitespace trimmed)
     *   "ETB 200"    → 200.0    ✓ (currency prefix stripped)
     *   "0.00"       → Failure  ✗ (zero rejected)
     *   ""           → Failure  ✗ (empty rejected)
     *   "abc"        → Failure  ✗ (non-numeric rejected)
     *   "99999999"   → Failure  ✗ (unreasonably large rejected)
     */
    fun parse(raw: String): AmountResult {
        if (raw.isBlank()) return AmountResult.Failure("Empty amount string")

        val sanitized = raw
            .trim()
            .removePrefix("-")          // strip negative sign — type carries direction
            .replace(Regex("[,\\s]"), "") // remove commas and whitespace
            .replace(Regex("^[A-Za-z]+\\s*"), "") // strip currency prefix (ETB, USD, etc.)
            .trim()

        if (sanitized.isEmpty()) return AmountResult.Failure("Amount is empty after sanitization: '$raw'")

        val value = sanitized.toDoubleOrNull()
            ?: return AmountResult.Failure("Cannot parse '$sanitized' as a number (raw: '$raw')")
                .also { Log.w(TAG, "Amount parse failure: raw='$raw' sanitized='$sanitized'") }

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
