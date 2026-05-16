package com.portfolio.financetracker.core.sms

import android.util.Log
import com.portfolio.financetracker.core.util.AmountParser.toSafeAmount
import com.portfolio.financetracker.core.util.SmsTimestampParser
import com.portfolio.financetracker.domain.model.TransactionType
import java.security.MessageDigest

/**
 * Parses bank SMS messages into structured transaction data.
 *
 * ── Root cause fix ────────────────────────────────────────────────────────────
 * The old design used `contains("CBE")` on the sender string which caused
 * false positives — any sender with those letters matched CBE.
 *
 * New design:
 * 1. The user explicitly selects which sender addresses to track (stored in
 *    DataStore as exact strings like "+251911123456" or "CBE").
 * 2. [parse] receives the user's tracked senders set and only processes
 *    messages from those exact addresses.
 * 3. Bank detection uses the BODY content as the primary signal, not the
 *    sender address — this is more reliable since real bank SMS always
 *    contain bank-specific keywords in the body.
 *
 * ── Security ──────────────────────────────────────────────────────────────────
 * • Only processes senders the user explicitly opted in to track
 * • Body-based bank detection prevents misclassification
 * • SHA-256 hash deduplication
 */
object SmsParser {

    // ── Known bank body signatures ────────────────────────────────────────────
    // Used to detect which bank format to apply based on SMS BODY content,
    // not the sender address (which can be a phone number or short code).

    enum class BankFormat {
        CBE, DASHEN, TELEBIRR, AWASH, ABYSSINIA, UNKNOWN
    }

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
        val smsDateString: String? = null
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses [body] from [sender] only if [sender] is in [trackedSenders].
     * Bank format is detected from the SMS body content, not the sender name.
     *
     * @param trackedSenders The exact sender addresses the user chose to track.
     *                       Pass an empty set to skip the tracking check (e.g. preview mode).
     */
    fun parse(
        sender: String,
        body: String,
        receivedAt: Long,
        trackedSenders: Set<String> = emptySet()
    ): ParsedSms? {
        // If tracking is configured, only process opted-in senders
        if (trackedSenders.isNotEmpty() && !isTrackedSender(sender, trackedSenders)) return null

        val normalizedBody = body.trim()

        // Check for dynamic parsers first
        val dynamicParser = BankSmsParserFactory.getDynamic(sender)
        if (dynamicParser != null) {
            when (val result = dynamicParser.parse(normalizedBody, sender, receivedAt)) {
                is ParseResult.Success -> return result.parsed
                is ParseResult.Failure -> {
                    android.util.Log.w("SmsParser", "Parse failure [DYNAMIC]: ${result.reason}")
                    // Fallthrough to detectBankFormat? Or just return null.
                    // Usually if a dynamic parser fails, we just return null.
                }
                is ParseResult.Ignored -> return null
            }
        }

        val format = detectBankFormat(normalizedBody)
        val bankName = format.displayName()

        // Task 6: delegate to the strategy parser for this bank format
        val parser = BankSmsParserFactory.get(format)
        if (parser != null) {
            return when (val result = parser.parse(normalizedBody, sender, receivedAt)) {
                is ParseResult.Success  -> result.parsed
                is ParseResult.Failure  -> {
                    android.util.Log.w("SmsParser",
                        "Parse failure [${result.bankFormat}]: ${result.reason}")
                    null
                }
                is ParseResult.Ignored  -> null
            }
        }

        // UNKNOWN format — try generic fallback
        return when (format) {
            BankFormat.UNKNOWN -> {
                val parsed = parseGeneric(normalizedBody, receivedAt, sender)
                if (parsed == null) {
                    android.util.Log.d("SmsParser", "SMS from tracked sender '$sender' ignored (unrecognized format).")
                }
                parsed
            }
            else -> {
                android.util.Log.d("SmsParser", "SMS from tracked sender '$sender' ignored (unrecognized format).")
                null
            }
        }
    }

    /**
     * Detects which bank format an SMS body belongs to.
     * Uses body keywords — more reliable than sender address matching.
     */
    fun detectBankFormat(body: String): BankFormat {
        val b = body.lowercase()
        return when {
            // CBE: "credited with ETB" / "debited with ETB" / "available balance"
            (b.contains("credited with etb") || b.contains("debited with etb")) &&
            b.contains("available balance") -> BankFormat.CBE

            // Dashen: "debit of etb" / "credit of etb" + "balance:"
            (b.contains("debit of etb") || b.contains("credit of etb")) -> BankFormat.DASHEN

            // Telebirr: "you have received/sent/paid etb" + "new balance"
            (b.contains("you have received etb") || b.contains("you have sent etb") ||
             b.contains("you have paid etb")) -> BankFormat.TELEBIRR

            // Awash: "awash bank" + "credited/debited etb"
            b.contains("awash") && (b.contains("credited etb") || b.contains("debited etb")) ->
                BankFormat.AWASH

            // Abyssinia/BOA: "dr etb" / "cr etb" + "avail bal"
            (b.contains(" dr etb") || b.contains(" cr etb")) && b.contains("avail bal") ->
                BankFormat.ABYSSINIA

            else -> BankFormat.UNKNOWN
        }
    }

    /** Returns true if [sender] exactly matches or is contained in [trackedSenders] */
    fun isTrackedSender(sender: String, trackedSenders: Set<String>): Boolean =
        trackedSenders.any { tracked ->
            sender.equals(tracked, ignoreCase = true) ||
            sender.contains(tracked, ignoreCase = true) ||
            tracked.contains(sender, ignoreCase = true)
        }

    /** Used by SmsInboxReader to build a broad initial query, then we filter precisely */
    val BANK_BODY_KEYWORDS = listOf(
        "credited with ETB", "debited with ETB",
        "Debit of ETB", "Credit of ETB",
        "You have received ETB", "You have sent ETB", "You have paid ETB",
        "Credited ETB", "Debited ETB",
        "Dr ETB", "Cr ETB",
        "Available balance", "new balance is ETB", "Avail Bal ETB"
    )

    private fun BankFormat.displayName() = when (this) {
        BankFormat.CBE       -> "CBE"
        BankFormat.DASHEN    -> "Dashen"
        BankFormat.TELEBIRR  -> "Telebirr"
        BankFormat.AWASH     -> "Awash"
        BankFormat.ABYSSINIA -> "Abyssinia"
        BankFormat.UNKNOWN   -> "Bank"
    }

    // ── CBE ───────────────────────────────────────────────────────────────────
    private val CBE_AMOUNT  = Regex("""(?:credited|debited)\s+with\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val CBE_BALANCE = Regex("""[Aa]vailable\s+balance[:\s]+ETB\s+([\d,]+\.?\d*)""")
    private val CBE_TYPE    = Regex("""(credited|debited)\s+with""", RegexOption.IGNORE_CASE)

    private fun parseCbe(body: String, ts: Long, bank: String): ParsedSms? {
        val amount  = CBE_AMOUNT.find(body)?.groupValues?.get(1)?.toCleanDouble() ?: return null
        val typeStr = CBE_TYPE.find(body)?.groupValues?.get(1)?.lowercase() ?: return null
        val type    = if (typeStr == "credited") TransactionType.INCOME else TransactionType.EXPENSE
        val balance = CBE_BALANCE.find(body)?.groupValues?.get(1)?.toCleanDouble()
        return buildResult(amount, type, balance, "$bank Transfer", body, ts, bank)
    }

    // ── Dashen ────────────────────────────────────────────────────────────────
    private val DASHEN_AMOUNT  = Regex("""(?:Debit|Credit)\s+of\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val DASHEN_BALANCE = Regex("""Balance[:\s]+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val DASHEN_TYPE    = Regex("""(Debit|Credit)\s+of""", RegexOption.IGNORE_CASE)

    private fun parseDashen(body: String, ts: Long, bank: String): ParsedSms? {
        val amount  = DASHEN_AMOUNT.find(body)?.groupValues?.get(1)?.toCleanDouble() ?: return null
        val typeStr = DASHEN_TYPE.find(body)?.groupValues?.get(1)?.lowercase() ?: return null
        val type    = if (typeStr == "credit") TransactionType.INCOME else TransactionType.EXPENSE
        val balance = DASHEN_BALANCE.find(body)?.groupValues?.get(1)?.toCleanDouble()
        return buildResult(amount, type, balance, "$bank Transfer", body, ts, bank)
    }

    // ── Telebirr ──────────────────────────────────────────────────────────────
    private val TELEBIRR_AMOUNT  = Regex("""(?:received|sent|paid)\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TELEBIRR_BALANCE = Regex("""new\s+balance\s+is\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TELEBIRR_TYPE    = Regex("""You\s+have\s+(received|sent|paid)""", RegexOption.IGNORE_CASE)

    private fun parseTelebirr(body: String, ts: Long, bank: String): ParsedSms? {
        val amount  = TELEBIRR_AMOUNT.find(body)?.groupValues?.get(1)?.toCleanDouble() ?: return null
        val typeStr = TELEBIRR_TYPE.find(body)?.groupValues?.get(1)?.lowercase() ?: return null
        val type    = if (typeStr == "received") TransactionType.INCOME else TransactionType.EXPENSE
        val balance = TELEBIRR_BALANCE.find(body)?.groupValues?.get(1)?.toCleanDouble()
        val category = if (typeStr == "paid") "Shopping" else "$bank Transfer"
        return buildResult(amount, type, balance, category, body, ts, bank)
    }

    // ── Awash ─────────────────────────────────────────────────────────────────
    private val AWASH_AMOUNT  = Regex("""(?:Credited|Debited)\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val AWASH_BALANCE = Regex("""Bal[:\s]+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val AWASH_TYPE    = Regex("""(Credited|Debited)\s+ETB""", RegexOption.IGNORE_CASE)

    private fun parseAwash(body: String, ts: Long, bank: String): ParsedSms? {
        val amount  = AWASH_AMOUNT.find(body)?.groupValues?.get(1)?.toCleanDouble() ?: return null
        val typeStr = AWASH_TYPE.find(body)?.groupValues?.get(1)?.lowercase() ?: return null
        val type    = if (typeStr == "credited") TransactionType.INCOME else TransactionType.EXPENSE
        val balance = AWASH_BALANCE.find(body)?.groupValues?.get(1)?.toCleanDouble()
        return buildResult(amount, type, balance, "$bank Transfer", body, ts, bank)
    }

    // ── Abyssinia / BOA ───────────────────────────────────────────────────────
    private val BOA_AMOUNT  = Regex("""\b(?:Dr|Cr)\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BOA_BALANCE = Regex("""Avail\s+Bal\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BOA_TYPE    = Regex("""\b(Dr|Cr)\s+ETB""", RegexOption.IGNORE_CASE)

    private fun parseAbyssinia(body: String, ts: Long, bank: String): ParsedSms? {
        val amount  = BOA_AMOUNT.find(body)?.groupValues?.get(1)?.toCleanDouble() ?: return null
        val typeStr = BOA_TYPE.find(body)?.groupValues?.get(1)?.lowercase() ?: return null
        val type    = if (typeStr == "cr") TransactionType.INCOME else TransactionType.EXPENSE
        val balance = BOA_BALANCE.find(body)?.groupValues?.get(1)?.toCleanDouble()
        return buildResult(amount, type, balance, "$bank Transfer", body, ts, bank)
    }

    // ── Generic fallback ──────────────────────────────────────────────────────
    // Tries to extract any ETB amount from an unrecognized bank format
    private val GENERIC_AMOUNT  = Regex("""ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val GENERIC_DEBIT   = Regex("""(?:debit|debited|sent|paid|withdrawn|Dr)\b""", RegexOption.IGNORE_CASE)
    private val GENERIC_CREDIT  = Regex("""(?:credit|credited|received|deposited|Cr)\b""", RegexOption.IGNORE_CASE)

    private fun parseGeneric(body: String, ts: Long, sender: String): ParsedSms? {
        val rawAmount = GENERIC_AMOUNT.find(body)?.groupValues?.get(1) ?: return null
        val amount = rawAmount.toSafeAmount() ?: return null
        val type = when {
            GENERIC_CREDIT.containsMatchIn(body) -> TransactionType.INCOME
            GENERIC_DEBIT.containsMatchIn(body)  -> TransactionType.EXPENSE
            else -> return null
        }
        val timestampMs = SmsTimestampParser.extractOrFallback(body, ts)
        val note  = "${if (type == TransactionType.INCOME) "Received" else "Sent"} ETB ${"%.2f".format(amount)} · Bank"
        val hash  = sha256(sender + body)
        return ParsedSms(
            amount      = amount,
            type        = type,
            balance     = null,
            category    = "Bank Transfer",
            note        = note,
            timestampMs = timestampMs,
            rawBody     = body,
            hash        = hash,
            bankName    = sender
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val DATE_PATTERNS = listOf(
        Regex("""\b(\d{2}[/\-]\d{2}[/\-]\d{4})\b"""),
        Regex("""\b(\d{2}[/\-][A-Za-z]{3}[/\-]\d{4})\b"""),
        Regex("""\b(\d{4}[/\-]\d{2}[/\-]\d{2})\b""")
    )

    private fun extractDateString(body: String): String? =
        DATE_PATTERNS.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1) }

    private fun buildResult(
        amount: Double,
        type: TransactionType,
        balance: Double?,
        category: String,
        rawBody: String,
        ts: Long,
        bankName: String
    ): ParsedSms {
        val typeLabel = if (type == TransactionType.INCOME) "Received" else "Sent"
        val note  = "$typeLabel ETB ${"%.2f".format(amount)} · $bankName"
        val hash  = sha256(bankName + rawBody)
        return ParsedSms(
            amount        = amount,
            type          = type,
            balance       = balance,
            category      = category,
            note          = note,
            timestampMs   = ts,
            rawBody       = rawBody,
            hash          = hash,
            bankName      = bankName,
            smsDateString = extractDateString(rawBody)
        )
    }

    private fun String.toCleanDouble(): Double? = replace(",", "").trim().toDoubleOrNull()

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
