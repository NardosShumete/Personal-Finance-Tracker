package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

object AwashSmsParser : BankSmsParser {
    override val bankName = "Awash"
    override val format   = SmsParser.BankFormat.AWASH

    override fun isSenderMatch(sender: String): Boolean {
        val s = sender.lowercase()
        return s.contains("awash") || s == "8040"
    }

    private val BALANCE_RE = Regex("""Bal[:\s]+ETB\s+([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val typeResult = com.portfolio.financetracker.core.sms.TemplateExtractor.extractType(body)
                ?: return ParseResult.Failure("Awash: type not found", body, format)
            
            val type = typeResult.first
            val keyword = typeResult.second

            val rawAmount = com.portfolio.financetracker.core.sms.TemplateExtractor.extractAmount(body, keyword)
                ?: return ParseResult.Failure("Awash: amount not found or unsafe", body, format)

            val balance = BALANCE_RE.find(body)?.groupValues?.get(1)
            buildParsedSms(rawAmount, type, balance, "$bankName Transfer", body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("Awash: unexpected error — ${e.message}", body, format)
        }
    }
}
