package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.core.sms.parsers.AbyssiniaSmsParser
import com.portfolio.financetracker.core.sms.parsers.AwashSmsParser
import com.portfolio.financetracker.core.sms.parsers.CbeSmsParser
import com.portfolio.financetracker.core.sms.parsers.DashenSmsParser
import com.portfolio.financetracker.core.sms.parsers.TelebirrSmsParser

/**
 * Extensible registry of all bank SMS parsers.
 *
 * WHY a separate registry instead of just the factory map:
 * • Single place to add a new bank — implement [BankSmsParser], add one line here.
 * • Supports runtime registration for user-defined custom banks.
 * • Provides a list of all registered parsers for discovery/testing.
 * • Decouples format detection from parser lookup — the registry owns both.
 *
 * Adding a new bank:
 *   1. Create `parsers/MyBankSmsParser.kt` implementing [BankSmsParser]
 *   2. Add a new entry to [SmsParser.BankFormat] enum
 *   3. Add the format detection rule to [SmsParser.detectBankFormat]
 *   4. Register here: `SmsParser.BankFormat.MY_BANK to MyBankSmsParser`
 *
 * Thread safety: the built-in registry is immutable after class load.
 * Dynamic parsers use a synchronized map.
 */
object SmsParserRegistry {

    // ── Built-in parsers (immutable, compiled at class load) ──────────────────
    private val builtIn: Map<SmsParser.BankFormat, BankSmsParser> = mapOf(
        SmsParser.BankFormat.CBE       to CbeSmsParser,
        SmsParser.BankFormat.DASHEN    to DashenSmsParser,
        SmsParser.BankFormat.TELEBIRR  to TelebirrSmsParser,
        SmsParser.BankFormat.AWASH     to AwashSmsParser,
        SmsParser.BankFormat.ABYSSINIA to AbyssiniaSmsParser
    )

    // ── Dynamic parsers (user-defined custom banks, mutable) ──────────────────
    // Key = lowercase sender address for fast lookup
    @Volatile
    private var dynamic: Map<String, BankSmsParser> = emptyMap()

    // ── Lookup ────────────────────────────────────────────────────────────────

    /**
     * Returns the built-in parser for [format], or null if none registered.
     * O(1) map lookup.
     */
    fun getBuiltIn(format: SmsParser.BankFormat): BankSmsParser? = builtIn[format]

    /**
     * Returns the built-in parser whose sender matches [sender].
     */
    fun getBuiltInBySender(sender: String): BankSmsParser? {
        return builtIn.values.firstOrNull { it.isSenderMatch(sender) }
    }

    /**
     * Returns a dynamic (user-defined) parser whose sender key matches [sender].
     * Matching is case-insensitive substring — handles both short codes and
     * full phone numbers.
     */
    fun getDynamic(sender: String): BankSmsParser? {
        val lowerSender = sender.lowercase()
        return dynamic.entries.firstOrNull { (key, _) ->
            lowerSender.contains(key) || key.contains(lowerSender)
        }?.value
    }

    /**
     * Replaces the dynamic parser map with parsers built from [customBanks].
     * Called whenever the user adds/removes/edits a custom bank.
     * Thread-safe via @Volatile + full replacement (no partial mutation).
     */
    fun setDynamicParsers(parsers: Map<String, BankSmsParser>) {
        dynamic = parsers
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    /** All registered built-in parsers — useful for testing and diagnostics. */
    fun allBuiltIn(): List<BankSmsParser> = builtIn.values.toList()

    /** All registered dynamic parsers. */
    fun allDynamic(): List<BankSmsParser> = dynamic.values.toList()

    /** All parsers (built-in + dynamic). */
    fun all(): List<BankSmsParser> = allBuiltIn() + allDynamic()

    /** Returns the bank name for a given format, or null if not registered. */
    fun bankNameFor(format: SmsParser.BankFormat): String? = builtIn[format]?.bankName
}
