package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

object AwashSmsParser : BankSmsParser {
    override val bankName = "Awash"
    override val format   = SmsParser.BankFormat.AWASH

    private val AMOUNT_RE  = Regex("""(?:Credited|Debited)\s+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val BALANCE_RE = Regex("""Bal[:\s]+ETB\s+([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TYPE_RE    = Regex("""(Credited|Debited)\s+ETB""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val rawAmount = AMOUNT_RE.find(body)?.groupValues?.get(1)
                ?: return ParseResult.Failure("Awash: amount not found", body, format)
            val typeStr = TYPE_RE.find(body)?.groupValues?.get(1)?.lowercase()
                ?: return ParseResult.Failure("Awash: type not found", body, format)
            val type    = if (typeStr == "credited") TransactionType.INCOME else TransactionType.EXPENSE
            val balance = BALANCE_RE.find(body)?.groupValues?.get(1)
            buildParsedSms(rawAmount, type, balance, "$bankName Transfer", body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("Awash: unexpected error — ${e.message}", body, format)
        }
    }
}
