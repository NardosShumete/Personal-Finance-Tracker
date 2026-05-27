package com.portfolio.financetracker.core.sms

import com.portfolio.financetracker.domain.model.TransactionType

object ValidationEngine {

    /**
     * Validates a parsed SMS and assigns a confidence score.
     * 
     * @param amount The extracted transaction amount
     * @param type The transaction type (INCOME/EXPENSE)
     * @param body The raw SMS body
     * @return Pair of (Confidence Score: Double, Reason: String)
     */
    fun validate(amount: Double, type: TransactionType, body: String): Pair<Double, String> {
        val lowerBody = body.lowercase()
        var score = 1.0
        var reason = "OK"

        // Rule 1: Amount must be positive
        if (amount <= 0) {
            return 0.0 to "Amount must be positive"
        }

        // Rule 2: Amount must be realistic (e.g., less than 100,000,000 ETB)
        if (amount > 100_000_000) {
            score -= 0.5
            reason = "Unusually high amount"
        }

        // Rule 3: Missing keywords
        // If we expect income but don't see typical credit keywords
        if (type == TransactionType.INCOME && 
            !lowerBody.contains("credit") && 
            !lowerBody.contains("receive") && 
            !lowerBody.contains("deposit")) {
            score -= 0.3
            reason = if (reason == "OK") "Missing income keyword" else "$reason, Missing income keyword"
        }

        // If we expect expense but don't see typical debit keywords
        if (type == TransactionType.EXPENSE && 
            !lowerBody.contains("debit") && 
            !lowerBody.contains("withdrawn") && 
            !lowerBody.contains("paid") &&
            !lowerBody.contains("sent") &&
            !lowerBody.contains("purchase")) {
            score -= 0.3
            reason = if (reason == "OK") "Missing expense keyword" else "$reason, Missing expense keyword"
        }

        // Make sure score is between 0.0 and 1.0
        score = score.coerceIn(0.0, 1.0)
        
        return score to reason
    }
}
