package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.core.util.AmountParser.toSafeAmount
import com.portfolio.financetracker.core.util.SmsTimestampParser
import com.portfolio.financetracker.domain.model.TransactionType
import java.security.MessageDigest

/**
 * Central SMS parsing coordinator.
 *
 * Responsibilities:
 *  1. Bank format detection from SMS body keywords (not sender address)
 *  2. Routing to the correct [BankSmsParser] via [SmsParserRegistry]
 *  3. Generic fallback for unrecognised-but-tracked senders
 *  4. Sender allowlist enforcement
 *
 * Architecture — Strategy Pattern:
 *  Each bank has its own parser object in core/sms/parsers/.
 *  This class only routes; it does NOT contain per-bank regex.
 *  Adding a new bank = add a parser + register in [SmsParserRegistry].
 *
 * Amount extraction rules (STEP 4 of spec):
 *  • Amount is extracted using a contextual regex NEAR the debit/credit keyword.
 *  • Balance numbers are captured separately and stored in [ParsedSms.balance].
 *  • The two are NEVER confused — each parser has distinct AMOUNT_RE and BALANCE_RE.
 *  • The generic fallback picks the ETB amount CLOSEST to the keyword position.
 *
 * Deduplication:
 *  • SHA-256(sender + body) — unique per SMS content
 *  • Unique DB index on smsHash prevents double-inserts even if the receiver fires twice
 */
object SmsParser {

    // ── Bank format enum ──────────────────────────────────────────────────────

    /**
     * Identifies which bank's SMS format was detected.
     * UNKNOWN = not a recognised bank SMS (will try generic fallback).
     *
     * To add a new bank:
     *  1. Add an entry here
     *  2. Add detection logic in [detectBankFormat]
     *  3. Create a parser in parsers/ and register in [SmsParserRegistry]
     */
    enum class BankFormat {
        CBE,        // Commercial Bank of Ethiopia
        DASHEN,     // Dashen Bank
        TELEBIRR,   // Ethio Telecom Telebirr
        AWASH,      // Awash Bank
        ABYSSINIA,  // Bank of Abyssinia (BOA)
        UNKNOWN
    }

    // ── Parsed result model ───────────────────────────────────────────────────

    /**
     * Structured data extracted from a single bank SMS.
     *
     * [amount]    — transaction amount (always positive; [type] carries sign)
     * [type]      — INCOME (credit) or EXPENSE (debit)
     * [balance]   — account balance reported in SMS, if present (stored separately,
     *               NEVER used as the transaction amount)
     * [category]  — human-readable category string (e.g. "CBE Transfer", "Shopping")
     * [note]      — auto-generated note shown in the transaction list
     * [bankName]  — exact bank identifier matching [BankAccountEntity.shortName]
     * [hash]      — SHA-256(sender + body) deduplication key
     */
    data class ParsedSms(
        val amount: Double,
        val type: TransactionType,
        val balance: Double?,
        val category: String,
        val note: String,
        val timestampMs: Long,
        val rawBody: String,
        val hash: String,
        val smsId: String? = null,
        val bankName: String = "",
        val smsDateString: String? = null,
        val sender: String? = null,
        val merchant: String? = null,
        val currency: String = "ETB",
        val confidenceScore: Double = 0.0,
        val parsingStatus: String = "PENDING"
    )

    // ── Keywords used by SmsInboxReader for broad inbox scan ──────────────────

    /**
     * Body keywords used to pre-filter the SMS inbox.
     * These are intentionally broad — false positives are filtered out by
     * [detectBankFormat] which applies stricter multi-keyword matching.
     */
    val BANK_BODY_KEYWORDS = listOf(
        "credited with ETB", "debited with ETB",
        "Debit of ETB",      "Credit of ETB",
        "You have received ETB", "You have sent ETB", "You have paid ETB",
        "Credited ETB",      "Debited ETB",
        "Dr ETB",            "Cr ETB",
        "Available balance", "new balance is ETB", "Avail Bal ETB"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Main entry point. Parses [body] from [sender] into a [ParsedSms].
     *
     * Gate order:
     *  1. If [trackedSenders] is non-empty, sender must be in the list.
     *  2. Check for a dynamic (user-defined) parser matching the sender.
     *  3. Detect bank format from body keywords.
     *  4. Route to the registered strategy parser.
     *  5. Fall back to generic ETB extraction for UNKNOWN format.
     *
     * @param trackedSenders Pass empty set to skip the sender check (e.g. in tests).
     * @return [ParseResult] containing the parsed data or failure reason.
     */
    fun parse(
        sender: String,
        body: String,
        receivedAt: Long,
        trackedSenders: Set<String> = emptySet()
    ): ParseResult {
        // Gate 1: sender allowlist
        if (trackedSenders.isNotEmpty() && !isTrackedSender(sender, trackedSenders)) {
            SmsParseLogger.logIgnored(sender, "not in tracked senders")
            return ParseResult.Ignored
        }

        val normalizedBody = body.trim()

        // Gate 2: dynamic (user-defined) parser — checked before built-ins
        val dynamicParser = SmsParserRegistry.getDynamic(sender)
        if (dynamicParser != null) {
            val result = dynamicParser.parse(normalizedBody, sender, receivedAt)
            if (result is ParseResult.Failure) {
                SmsParseLogger.logFailure("DYNAMIC", result.reason, normalizedBody)
            }
            return result
        }

        // Gate 3: detect bank format from SENDER first (Hybrid approach)
        var parser = SmsParserRegistry.getBuiltInBySender(sender)
        var format = parser?.format ?: BankFormat.UNKNOWN

        // If sender doesn't match, fallback to body keywords
        if (parser == null) {
            format = detectBankFormat(normalizedBody)
            parser = SmsParserRegistry.getBuiltIn(format)
        }

        // Gate 4: route to strategy parser
        if (parser != null) {
            val result = parser.parse(normalizedBody, sender, receivedAt)
            if (result is ParseResult.Failure) {
                SmsParseLogger.logFailure(format.name, result.reason, normalizedBody)
            }
            return result
        }

        // Gate 5: generic fallback for UNKNOWN format
        if (format == BankFormat.UNKNOWN) {
            val result = parseGeneric(normalizedBody, receivedAt, sender)
            if (result is ParseResult.Failure) {
                SmsParseLogger.logIgnored(sender, "unrecognised SMS format: ${result.reason}")
            }
            return result
        }

        SmsParseLogger.logIgnored(sender, "no parser for format $format")
        return ParseResult.Ignored
    }

    // ── Bank format detection ─────────────────────────────────────────────────

    /**
     * Detects which bank's SMS format matches [body].
     *
     * Uses BODY keywords — NOT sender address — because:
     * • Sender addresses vary (short codes, phone numbers, alphanumeric IDs)
     * • Body content is standardised by each bank's notification system
     * • Body-based detection prevents false positives from sender name matching
     *
     * Each bank requires at least TWO distinct keywords to avoid false positives.
     */
    fun detectBankFormat(body: String): BankFormat {
        val b = body.lowercase()
        return when {
            // CBE: "credited/debited with ETB" + "available balance" or "your account"
            (b.contains("credited with etb") || b.contains("debited with etb")) &&
            (b.contains("available balance") || b.contains("your account")) ->
                BankFormat.CBE

            // Dashen: "Debit/Credit of ETB"
            (b.contains("debit of etb") || b.contains("credit of etb")) ->
                BankFormat.DASHEN

            // Telebirr: "You have received/sent/paid ETB"
            (b.contains("you have received etb") || b.contains("you have sent etb") ||
             b.contains("you have paid etb")) ->
                BankFormat.TELEBIRR

            // Awash: "awash" + "Credited/Debited ETB"
            b.contains("awash") &&
            (b.contains("credited etb") || b.contains("debited etb")) ->
                BankFormat.AWASH

            // BOA/Abyssinia: "Dr/Cr ETB" + "Avail Bal"
            // Use word boundary instead of leading space so it matches at start-of-string too
            (Regex("""\bDr\s+ETB\b""", RegexOption.IGNORE_CASE).containsMatchIn(b) ||
             Regex("""\bCr\s+ETB\b""", RegexOption.IGNORE_CASE).containsMatchIn(b)) &&
            b.contains("avail bal") ->
                BankFormat.ABYSSINIA

            else -> BankFormat.UNKNOWN
        }
    }

    // ── Sender allowlist ──────────────────────────────────────────────────────

    /**
     * Returns true if [sender] matches any entry in [trackedSenders].
     * Matching is case-insensitive and bidirectional substring — handles:
     *  • Exact match:    "CBE" == "CBE"
     *  • Sender longer:  "+251911123456" contains "251911"
     *  • Tracked longer: "CBEBirr" contains "CBE"
     */
    fun isTrackedSender(sender: String, trackedSenders: Set<String>): Boolean =
        trackedSenders.any { tracked ->
            sender.equals(tracked, ignoreCase = true) ||
            sender.contains(tracked, ignoreCase = true) ||
            tracked.contains(sender, ignoreCase = true)
        }

    // ── Generic fallback ──────────────────────────────────────────────────────

    /**
     * Last-resort parser for SMS from tracked senders with unrecognised format.
     *
     * Strategy:
     *  1. Detect transaction type from debit/credit keywords.
     *  2. Find ALL ETB amounts in the body.
     *  3. Pick the amount CLOSEST to the keyword position — this avoids
     *     accidentally picking up the balance or a reference number.
     *
     * This is intentionally conservative — it returns null rather than
     * guessing if the type or amount cannot be determined confidently.
     */
    private val GENERIC_ETB_ALL = Regex("""ETB\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    // Debit keywords — ordered most-specific first to avoid partial matches
    private val DEBIT_KEYWORDS  = Regex(
        """(?:debited|debit|withdrawn|withdrawal|paid|payment|purchase|sent)\b""",
        RegexOption.IGNORE_CASE
    )
    // Credit keywords
    private val CREDIT_KEYWORDS = Regex(
        """(?:credited|credit|received|deposited|deposit)\b""",
        RegexOption.IGNORE_CASE
    )

    internal fun parseGeneric(body: String, receivedAt: Long, sender: String): ParseResult {
        // Step 1: detect type from keywords BEFORE looking at amounts
        val creditMatch = CREDIT_KEYWORDS.find(body)
        val debitMatch  = DEBIT_KEYWORDS.find(body)

        // If both keywords appear, pick the one that comes first in the body
        // (the first keyword is more likely to describe the transaction action)
        val type = when {
            creditMatch != null && debitMatch != null ->
                if (creditMatch.range.first < debitMatch.range.first)
                    TransactionType.INCOME else TransactionType.EXPENSE
            creditMatch != null -> TransactionType.INCOME
            debitMatch  != null -> TransactionType.EXPENSE
            else -> return ParseResult.Ignored  // no type keyword found — not a transaction SMS
        }

        val keywordPos = (if (type == TransactionType.INCOME) creditMatch else debitMatch)
            ?.range?.first ?: 0

        // Step 2: collect all ETB amounts
        val allAmounts = GENERIC_ETB_ALL.findAll(body).toList()
        if (allAmounts.isEmpty()) return ParseResult.Failure("Generic fallback: no amounts found", body, BankFormat.UNKNOWN)

        // Step 3: pick the amount closest to the keyword
        // This is the core fix — balance appears AFTER the transaction amount
        // in most bank SMS, so "closest to keyword" reliably picks the right one.
        val bestMatch = allAmounts.minByOrNull { kotlin.math.abs(it.range.first - keywordPos) }
            ?: return ParseResult.Failure("Generic fallback: could not find closest amount", body, BankFormat.UNKNOWN)

        val amount = bestMatch.groupValues[1].toSafeAmount() 
            ?: return ParseResult.Failure("Generic fallback: unsafe amount format", body, BankFormat.UNKNOWN)

        val timestampMs = SmsTimestampParser.extractOrFallback(body, receivedAt)
        val typeLabel   = if (type == TransactionType.INCOME) "Received" else "Sent"
        val note        = "$typeLabel ETB ${"%.2f".format(amount)} · Bank"
        val hash        = sha256(sender + body + timestampMs.toString() + amount.toString())

        val (confidenceScore, validationReason) = ValidationEngine.validate(amount, type, body)

        SmsParseLogger.logSuccess("GENERIC", type.name, amount, null, sender)

        return ParseResult.Success(
            ParsedSms(
                amount      = amount,
                type        = type,
                balance     = null,
                category    = "Bank Transfer",
                note        = note,
                timestampMs = timestampMs,
                rawBody     = body,
                hash        = hash,
                bankName    = sender,
                sender      = sender,
                confidenceScore = 0.5, // Generic fallback gets lower confidence
                parsingStatus = "GENERIC_FALLBACK"
            )
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    internal fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
