package com.portfolio.financetracker.core.sms.parsers

import com.portfolio.financetracker.core.sms.BankSmsParser
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.buildParsedSms
import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import com.portfolio.financetracker.domain.model.TransactionType

/**
 * Parses SMS messages based on dynamically defined keywords from the database.
 */
class DynamicBankSmsParser(
    private val config: CustomBankEntity
) : BankSmsParser {

    override val bankName: String = config.name

    // Use UNKNOWN as the base format, but it will be managed dynamically
    override val format: SmsParser.BankFormat = SmsParser.BankFormat.UNKNOWN

    override fun parse(body: String, sender: String, receivedAt: Long): ParseResult {
        try {
            val lowerBody = body.lowercase()
            
            // Determine transaction type based on keywords
            val type = when {
                lowerBody.contains(config.creditKeyword.lowercase()) -> TransactionType.INCOME
                lowerBody.contains(config.debitKeyword.lowercase()) -> TransactionType.EXPENSE
                else -> return ParseResult.Failure(
                    reason = "No matching credit/debit keyword found for custom bank '${config.name}'",
                    rawBody = body,
                    bankFormat = format
                )
            }
            
            // Extract Amount — look for the keyword then grab the next ETB number,
            // or fall back to any ETB amount in the body.
            // We use a strict numeric pattern (digits, commas, optional decimal)
            // to avoid capturing punctuation or account numbers.
            val keyword = if (type == TransactionType.INCOME) config.creditKeyword else config.debitKeyword
            val amountRegexStr = "${Regex.escape(keyword)}\\s*(?:ETB)?\\s*([\\d,]+(?:\\.\\d+)?)"

            val amountMatch = Regex(amountRegexStr, RegexOption.IGNORE_CASE).find(body)
                ?: Regex("ETB\\s*([\\d,]+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(body)
                ?: Regex("([\\d,]+(?:\\.\\d+)?)\\s*ETB", RegexOption.IGNORE_CASE).find(body)

            val rawAmount = amountMatch?.groupValues?.get(1) ?: return ParseResult.Failure(
                reason = "Could not extract amount near keyword",
                rawBody = body,
                bankFormat = format
            )

            // Extract Balance (optional) — use strict numeric pattern
            var rawBalance: String? = null
            if (config.balanceKeyword.isNotBlank() && lowerBody.contains(config.balanceKeyword.lowercase())) {
                val balanceRegexStr = "${Regex.escape(config.balanceKeyword)}\\s*(?:ETB)?\\s*([\\d,]+(?:\\.\\d+)?)"
                val balanceMatch = Regex(balanceRegexStr, RegexOption.IGNORE_CASE).find(body)
                rawBalance = balanceMatch?.groupValues?.get(1)
            }

            return buildParsedSms(
                rawAmount = rawAmount,
                type = type,
                rawBalance = rawBalance,
                category = "${config.name} Transfer",
                body = body,
                sender = sender,
                receivedAt = receivedAt,
                bankName = config.name
            )
        } catch (e: Exception) {
            return ParseResult.Failure(
                reason = "Crash parsing dynamic bank ${config.name}: ${e.message}",
                rawBody = body,
                bankFormat = format
            )
        }
    }
}
