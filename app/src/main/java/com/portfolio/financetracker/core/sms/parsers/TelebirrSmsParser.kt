package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

/**
 * Telebirr (Ethio Telecom) SMS parser.
 *
 * Supported formats:
 *   "You have received ETB 200.00 from 0911XXXXXX.
 *    Your new balance is ETB 450.00. TxnID: 987654"
 *
 *   "You have sent ETB 150.00 to 0922XXXXXX.
 *    Your new balance is ETB 300.00."
 *
 *   "You have paid ETB 75.00 to Merchant XYZ.
 *    Your new balance is ETB 225.00."
 */
object TelebirrSmsParser : BankSmsParser {

    override val bankName = "Telebirr"
    override val format   = SmsParser.BankFormat.TELEBIRR

    private val AMOUNT_RE  = Regex("""(?:received|sent|paid)\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BALANCE_RE = Regex("""new\s+balance\s+is\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TYPE_RE    = Regex("""You\s+have\s+(received|sent|paid)""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val rawAmount = AMOUNT_RE.find(body)?.groupValues?.get(1)
                ?: return ParseResult.Failure("Telebirr: amount not found", body, format)

            val typeStr = TYPE_RE.find(body)?.groupValues?.get(1)?.lowercase()
                ?: return ParseResult.Failure("Telebirr: transaction type not found", body, format)

            val type     = if (typeStr == "received") TransactionType.INCOME else TransactionType.EXPENSE
            val balance  = BALANCE_RE.find(body)?.groupValues?.get(1)
            val category = if (typeStr == "paid") "Shopping" else "$bankName Transfer"

            buildParsedSms(rawAmount, type, balance, category, body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("Telebirr: unexpected error — ${e.message}", body, format)
        }
    }
}
