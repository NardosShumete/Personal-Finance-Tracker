package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.core.sms.SmsParseLogger
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
import com.portfolio.financetracker.domain.repository.TransactionRepository
import com.portfolio.financetracker.core.sms.ParseResult
import com.portfolio.financetracker.data.local.dao.FailedParseDao
import com.portfolio.financetracker.data.local.entity.FailedParseEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Processes a single incoming SMS.
 *
 * Gate logic (in order):
 * 1. SMS tracking must be enabled by the user.
 * 2. The SMS body must match a known bank format (body-based detection).
 * 3. If the user has configured a tracked-senders list, the sender must be in it.
 *    If no senders are configured yet, we allow any body-matched SMS through
 *    so that CBE/BOA/Telebirr all work without requiring the setup screen first.
 */
class ProcessSmsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val dataStoreManager: DataStoreManager,
    private val failedParseDao: FailedParseDao
) {
    suspend operator fun invoke(
        sender: String,
        body: String,
        receivedAt: Long
    ): Result<Boolean> = runCatching {

        // Gate 1: SMS tracking must be enabled
        val isEnabled = dataStoreManager.isSmsTrackingEnabled.first()
        if (!isEnabled) return Result.success(false)

        // Gate 2: body must match a known bank format
        // This is the primary filter — if the body doesn't look like a bank SMS,
        // we drop it immediately without any DB or DataStore access.
        val format = SmsParser.detectBankFormat(body)
        if (format == SmsParser.BankFormat.UNKNOWN) return Result.success(false)

        // Gate 3: sender allowlist (optional)
        // If the user has gone through the SMS setup screen and selected specific
        // senders, only process those. If the list is empty (setup not done yet),
        // allow all body-matched SMS through so banks work out of the box.
        val trackedSenders = dataStoreManager.trackedSmsSenders.first()
        if (trackedSenders.isNotEmpty() && !SmsParser.isTrackedSender(sender, trackedSenders)) {
            return Result.success(false)
        }

        // Parse — pass empty set so the sender check inside SmsParser is skipped
        // (we already checked it above with better logic)
        val parseResult = SmsParser.parse(sender, body, receivedAt, emptySet())

        when (parseResult) {
            is ParseResult.Success -> {
                val parsed = parseResult.parsed
                SmsParseLogger.i("ProcessSmsUseCase: parsed ${parsed.bankName} ${parsed.type} ${parsed.amount}")

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
                    isPending  = parsed.parsingStatus != "AUTO_VERIFIED",
                    bankName   = parsed.bankName.ifBlank { null },
                    sender     = parsed.sender,
                    currency   = parsed.currency,
                    merchant   = parsed.merchant,
                    confidenceScore = parsed.confidenceScore,
                    parsingStatus = parsed.parsingStatus
                )

                repository.insertFromSmsIfNotDuplicate(transaction)
            }
            is ParseResult.Failure -> {
                val entity = FailedParseEntity(
                    sender = sender,
                    rawBody = body,
                    date = receivedAt,
                    reason = parseResult.reason
                )
                failedParseDao.insertFailedParse(entity)
                false
            }
            is ParseResult.Ignored -> {
                false
            }
        }
    }
}
