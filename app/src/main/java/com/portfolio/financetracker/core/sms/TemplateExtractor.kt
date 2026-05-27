package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.domain.model.TransactionType

object TemplateExtractor {

    private val GENERIC_AMOUNT_PATTERN = Regex("""(?:ETB|Birr)?\s*([\d,]+(?:\.\d+)?)\s*(?:ETB|Birr)?""", RegexOption.IGNORE_CASE)
    
    /**
     * Extracts the transaction amount by finding all numbers in the text and
     * picking the one closest to the given transaction keyword, while
     * strictly excluding numbers that are near a "balance" keyword.
     */
    fun extractAmount(body: String, keyword: String): String? {
        val keywordMatch = Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE).find(body) ?: return null
        val keywordPos = keywordMatch.range.first

        val balanceMatch = Regex("""balance\s*(?:is|:)?\s*(?:ETB|Birr)?""", RegexOption.IGNORE_CASE).find(body)
        val balancePos = balanceMatch?.range?.first ?: Int.MAX_VALUE

        val allAmounts = GENERIC_AMOUNT_PATTERN.findAll(body).toList()
        
        // Filter out amounts that appear after or very close to the balance keyword
        val validAmounts = allAmounts.filter { amountMatch ->
            val amountPos = amountMatch.range.first
            // If balance is present and amount is near or after balance, ignore it
            if (balancePos != Int.MAX_VALUE && amountPos >= balancePos - 5) {
                false
            } else {
                true
            }
        }

        if (validAmounts.isEmpty()) return null

        // Pick the one closest to the transaction keyword
        val bestMatch = validAmounts.minByOrNull { kotlin.math.abs(it.range.first - keywordPos) }
        return bestMatch?.groupValues?.get(1)
    }

    /**
     * Determines transaction type based on common banking keywords.
     */
    fun extractType(body: String): Pair<TransactionType, String>? {
        val lowerBody = body.lowercase()
        
        // Debit keywords
        val debitWords = listOf("debited", "debit", "withdrawn", "withdrawal", "paid", "payment", "purchase", "sent", "dr")
        for (word in debitWords) {
            val match = Regex("""\b$word\b""").find(lowerBody)
            if (match != null) {
                return TransactionType.EXPENSE to match.value
            }
        }
        
        // Credit keywords
        val creditWords = listOf("credited", "credit", "received", "deposited", "deposit", "cr")
        for (word in creditWords) {
            val match = Regex("""\b$word\b""").find(lowerBody)
            if (match != null) {
                return TransactionType.INCOME to match.value
            }
        }
        
        return null
    }
}
