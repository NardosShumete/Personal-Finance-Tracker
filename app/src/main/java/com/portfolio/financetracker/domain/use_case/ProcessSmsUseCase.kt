package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Processes a single incoming SMS.
 *
 * Only processes the SMS if:
 * 1. SMS tracking is enabled by the user
 * 2. The sender is in the user's tracked senders list
 * 3. The SMS body matches a known bank transaction format
 */
class ProcessSmsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val dataStoreManager: DataStoreManager
) {
    suspend operator fun invoke(
        sender: String,
        body: String,
        receivedAt: Long
    ): Result<Boolean> = runCatching {
        // Check if SMS tracking is enabled
        val isEnabled = dataStoreManager.isSmsTrackingEnabled.first()
        if (!isEnabled) return Result.success(false)

        // Get the user's explicitly chosen senders
        val trackedSenders = dataStoreManager.trackedSmsSenders.first()
        if (trackedSenders.isEmpty()) return Result.success(false)

        // Parse — will return null if sender not tracked or body doesn't match
        val parsed = SmsParser.parse(sender, body, receivedAt, trackedSenders)
            ?: return Result.success(false)

        val transaction = Transaction(
            amount     = parsed.amount,
            category   = parsed.category,
            date       = parsed.timestampMs,
            type       = parsed.type,
            note       = parsed.note,
            source     = TransactionSource.SMS,
            rawSms     = parsed.rawBody,
            smsBalance = parsed.balance,
            smsHash    = parsed.hash,
            isPending  = true   // always requires user confirmation
        )

        repository.insertFromSmsIfNotDuplicate(transaction)
    }
}
