package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

object DashenSmsParser : BankSmsParser {
    override val bankName = "Dashen"
    override val format   = SmsParser.BankFormat.DASHEN

    private val AMOUNT_RE  = Regex("""(?:Debit|Credit)\s+of\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BALANCE_RE = Regex("""Balance[:\s]+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TYPE_RE    = Regex("""(Debit|Credit)\s+of""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val rawAmount = AMOUNT_RE.find(body)?.groupValues?.get(1)
                ?: return ParseResult.Failure("Dashen: amount not found", body, format)
            val typeStr = TYPE_RE.find(body)?.groupValues?.get(1)?.lowercase()
                ?: return ParseResult.Failure("Dashen: type not found", body, format)
            val type    = if (typeStr == "credit") TransactionType.INCOME else TransactionType.EXPENSE
            val balance = BALANCE_RE.find(body)?.groupValues?.get(1)
            buildParsedSms(rawAmount, type, balance, "$bankName Transfer", body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("Dashen: unexpected error — ${e.message}", body, format)
        }
    }
}
