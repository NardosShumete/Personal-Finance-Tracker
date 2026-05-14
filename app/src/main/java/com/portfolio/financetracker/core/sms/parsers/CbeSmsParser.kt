package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

/**
 * CBE (Commercial Bank of Ethiopia) SMS parser.
 *
 * Supported formats:
 *   "Your account XXXX1234 has been credited with ETB 1,500.00.
 *    Available balance: ETB 12,345.67. Date: 14/05/2026"
 *
 *   "Your account XXXX1234 has been debited with ETB 250.00.
 *    Available balance: ETB 12,095.67."
 *
 * Task 5 — crash protection:
 * Every regex operation is wrapped. If any step fails, ParseResult.Failure
 * is returned with a descriptive reason. The app never crashes.
 */
object CbeSmsParser : BankSmsParser {

    override val bankName = "CBE"
    override val format   = SmsParser.BankFormat.CBE

    // Regex compiled once at class load — not per-call (performance)
    private val AMOUNT_RE  = Regex("""(?:credited|debited)\s+with\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BALANCE_RE = Regex("""[Aa]vailable\s+balance[:\s]+ETB\s+([\d,]+\.?\d*)""")
    private val TYPE_RE    = Regex("""(credited|debited)\s+with""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val rawAmount = AMOUNT_RE.find(body)?.groupValues?.get(1)
                ?: return ParseResult.Failure("CBE: amount not found", body, format)

            val typeStr = TYPE_RE.find(body)?.groupValues?.get(1)?.lowercase()
                ?: return ParseResult.Failure("CBE: transaction type not found", body, format)

            val type    = if (typeStr == "credited") TransactionType.INCOME else TransactionType.EXPENSE
            val balance = BALANCE_RE.find(body)?.groupValues?.get(1)

            buildParsedSms(rawAmount, type, balance, "$bankName Transfer", body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("CBE: unexpected error — ${e.message}", body, format)
        }
    }
}
