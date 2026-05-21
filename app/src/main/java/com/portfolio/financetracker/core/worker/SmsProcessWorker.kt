package com.portfolio.financetracker.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.portfolio.financetracker.data.local.dao.BankAccountDao
import com.portfolio.financetracker.domain.use_case.ProcessSmsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that processes a single incoming bank SMS.
 *
 * ── Why WorkManager instead of goAsync() ─────────────────────────────────────
 * • Survives process death — if the phone restarts mid-parse, WorkManager
 *   retries automatically.
 * • Battery-aware — WorkManager respects Doze mode and battery saver.
 * • Guaranteed execution — work is persisted to disk before the receiver
 *   returns, so no SMS is ever lost.
 * • Hilt injection — the worker can receive the full DI graph cleanly.
 *
 * ── Retry policy ─────────────────────────────────────────────────────────────
 * Uses exponential backoff starting at 15 s. If the DB is locked or the
 * device is low on memory, the work retries up to 3 times before giving up.
 */
@HiltWorker
class SmsProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processSmsUseCase: ProcessSmsUseCase,
    private val bankAccountDao: BankAccountDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender     = inputData.getString(KEY_SENDER)     ?: return Result.failure()
        val body       = inputData.getString(KEY_BODY)       ?: return Result.failure()
        val receivedAt = inputData.getLong(KEY_RECEIVED_AT, 0L)

        // Gate: only process if SMS tracking is enabled at all.
        // We do NOT gate on smsSenderId matching here — that was the old approach
        // and caused CBE/Dashen/Awash to be silently dropped when the sender address
        // didn't exactly match the seeded smsSenderId string.
        //
        // The correct gate is body-based detection (detectBankFormat) which already
        // happens inside ProcessSmsUseCase → SmsParser.parse(). If the body doesn't
        // match any known bank format, the use case returns false and nothing is saved.
        //
        // The user's tracked-senders list (DataStore) is the real allowlist — it is
        // checked inside ProcessSmsUseCase. We only need to verify SMS tracking is on.
        val accounts = bankAccountDao.getAllBankAccounts().first()
        val smsTrackingEnabled = accounts.any { it.isConnected }
        if (!smsTrackingEnabled) {
            Log.d(TAG, "No banks connected — skipping SMS from $sender")
            return Result.success()
        }

        return try {
            processSmsUseCase(sender, body, receivedAt)
                .onSuccess { inserted ->
                    Log.i(TAG, if (inserted) "Inserted SMS from $sender"
                               else "Duplicate/non-bank SMS from $sender — skipped")
                }
                .onFailure { e -> Log.e(TAG, "SMS processing failed", e) }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker error — will retry", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG           = "SmsProcessWorker"
        private const val KEY_SENDER     = "sender"
        private const val KEY_BODY       = "body"
        private const val KEY_RECEIVED_AT = "received_at"
        private const val MAX_RETRIES   = 3

        /**
         * Enqueues a one-time SMS processing job.
         * Uses a unique name based on sender+timestamp to prevent duplicate
         * work items if the BroadcastReceiver fires twice for the same SMS.
         */
        fun enqueue(context: Context, sender: String, body: String, receivedAt: Long) {
            val data = workDataOf(
                KEY_SENDER      to sender,
                KEY_BODY        to body,
                KEY_RECEIVED_AT to receivedAt
            )
            val request = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)  // process even on low battery
                        .build()
                )
                .build()

            // KEEP_EXISTING prevents duplicate work for the same SMS
            WorkManager.getInstance(context).enqueueUniqueWork(
                "sms_${sender}_$receivedAt",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
