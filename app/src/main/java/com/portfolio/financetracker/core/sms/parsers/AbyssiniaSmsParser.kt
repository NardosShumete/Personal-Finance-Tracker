package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.domain.model.TransactionType

object AbyssiniaSmsParser : BankSmsParser {
    // bankName must match BankAccountEntity.shortName = "BOA" so that
    // transactions are correctly attributed to the Bank of Abyssinia card.
    override val bankName = "BOA"
    override val format   = SmsParser.BankFormat.ABYSSINIA

    override fun isSenderMatch(sender: String): Boolean {
        val s = sender.lowercase()
        return s.contains("boa") || s.contains("abyssinia") || s.contains("apollo")
    }

    private val BALANCE_RE = Regex("""Avail\s+Bal\s+ETB\s+([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        return try {
            val typeResult = com.portfolio.financetracker.core.sms.TemplateExtractor.extractType(body)
                ?: return ParseResult.Failure("Abyssinia: type not found", body, format)
            
            val type = typeResult.first
            val keyword = typeResult.second

            val rawAmount = com.portfolio.financetracker.core.sms.TemplateExtractor.extractAmount(body, keyword)
                ?: return ParseResult.Failure("Abyssinia: amount not found or unsafe", body, format)

            val balance = BALANCE_RE.find(body)?.groupValues?.get(1)
            buildParsedSms(rawAmount, type, balance, "$bankName Transfer", body, sender, receivedAt, bankName)
        } catch (e: Exception) {
            ParseResult.Failure("Abyssinia: unexpected error — ${e.message}", body, format)
        }
    }
}
