package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.core.sms.parsers.AbyssiniaSmsParser
import com.portfolio.financetracker.core.sms.parsers.AwashSmsParser
import com.portfolio.financetracker.core.sms.parsers.CbeSmsParser
import com.portfolio.financetracker.core.sms.parsers.DashenSmsParser
import com.portfolio.financetracker.core.sms.parsers.TelebirrSmsParser
import com.portfolio.financetracker.core.sms.parsers.DynamicBankSmsParser
import com.portfolio.financetracker.core.util.AmountParser.toSafeAmount
import com.portfolio.financetracker.core.util.SmsTimestampParser
import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import com.portfolio.financetracker.domain.model.TransactionType

/**
 * Task 6 — Strategy Pattern for bank parsers.
 *
 * WHY Strategy Pattern:
 * • Each bank has a completely different SMS format — a single monolithic
 *   parser becomes unmaintainable as banks are added.
 * • Strategy lets each bank parser be developed, tested, and deployed
 *   independently without touching other parsers.
 * • Adding a new bank = implementing one interface + registering in the factory.
 * • SOLID: Open/Closed — open for extension, closed for modification.
 *
 * Folder structure:
 *   core/sms/
 *     BankSmsParser.kt          ← this file (interface + factory)
 *     parsers/
 *       CbeSmsParser.kt
 *       DashenSmsParser.kt
 *       TelebirrSmsParser.kt
 *       AwashSmsParser.kt
 *       AbyssiniaSmsParser.kt
 */

// ── Strategy interface ────────────────────────────────────────────────────────

/**
 * Contract every bank parser must implement.
 * Each implementation is stateless and thread-safe.
 */
interface BankSmsParser {
    /** Human-readable bank name used in transaction category and notes. */
    val bankName: String

    /** The [SmsParser.BankFormat] this parser handles. */
    val format: SmsParser.BankFormat

    /**
     * Attempts to parse [body] as a transaction from this bank.
     *
     * @return [ParseResult.Success] if parsed successfully
     *         [ParseResult.Failure] if body matched this bank's format but data extraction failed
     *         [ParseResult.Ignored] should NOT be returned here — format detection happens upstream
     */
    fun parse(body: String, sender: String, receivedAt: Long): ParseResult
}

// ── Factory ───────────────────────────────────────────────────────────────────

/**
 * Returns the correct [BankSmsParser] for a given [SmsParser.BankFormat].
 * Registered parsers are singletons — no state, safe to share.
 */
object BankSmsParserFactory {

    private val parsers: Map<SmsParser.BankFormat, BankSmsParser> = mapOf(
        SmsParser.BankFormat.CBE       to CbeSmsParser,
        SmsParser.BankFormat.DASHEN    to DashenSmsParser,
        SmsParser.BankFormat.TELEBIRR  to TelebirrSmsParser,
        SmsParser.BankFormat.AWASH     to AwashSmsParser,
        SmsParser.BankFormat.ABYSSINIA to AbyssiniaSmsParser
    )

    private val dynamicParsers = mutableMapOf<String, BankSmsParser>()

    fun get(format: SmsParser.BankFormat): BankSmsParser? = parsers[format]

    fun getDynamic(sender: String): BankSmsParser? {
        return dynamicParsers.entries.firstOrNull { 
            sender.contains(it.key, ignoreCase = true) || it.key.contains(sender, ignoreCase = true)
        }?.value
    }

    fun setDynamicParsers(customBanks: List<CustomBankEntity>) {
        dynamicParsers.clear()
        customBanks.filter { it.isEnabled }.forEach { bank ->
            dynamicParsers[bank.senderAddress.lowercase()] = DynamicBankSmsParser(bank)
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

internal fun buildParsedSms(
    rawAmount: String,
    type: TransactionType,
    rawBalance: String?,
    category: String,
    body: String,
    sender: String,
    receivedAt: Long,
    bankName: String
): ParseResult {
    // Task 1: always use safe amount parsing
    val amount = rawAmount.toSafeAmount()
        ?: return ParseResult.Failure(
            reason     = "Could not parse amount '$rawAmount'",
            rawBody    = body,
            bankFormat = SmsParser.BankFormat.UNKNOWN
        )

    val balance = rawBalance?.toSafeAmount()  // null is fine — balance is optional

    // Task 4: prefer timestamp from SMS body over receive time
    val timestampMs = SmsTimestampParser.extractOrFallback(body, receivedAt)

    val typeLabel = if (type == TransactionType.INCOME) "Received" else "Sent"
    val note  = "$typeLabel ETB ${"%.2f".format(amount)} · $bankName"
    val hash  = sha256(sender + body)

    return ParseResult.Success(
        SmsParser.ParsedSms(
            amount        = amount,
            type          = type,
            balance       = balance,
            category      = category,
            note          = note,
            timestampMs   = timestampMs,
            rawBody       = body,
            hash          = hash,
            bankName      = bankName,
            smsDateString = null  // already consumed by SmsTimestampParser
        )
    )
}

private fun sha256(input: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
