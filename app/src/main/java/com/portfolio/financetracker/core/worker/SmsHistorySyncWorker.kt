package com.portfolio.financetracker.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.portfolio.financetracker.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * One-time WorkManager worker that backfills historical bank SMS.
 *
 * Triggered once when the user first enables SMS sync (from SmsPermissionCard).
 * Uses [ExistingWorkPolicy.KEEP] so tapping "Sync History" multiple times
 * doesn't create duplicate work.
 *
 * All inserted transactions are marked isPending=true so the user reviews
 * them in the Pending Review screen before they affect totals.
 */
@HiltWorker
class SmsHistorySyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TransactionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val limit = inputData.getInt(KEY_LIMIT, 200)
        return try {
            val count = repository.syncSmsHistory(context, limit) { processed, total ->
                setProgress(workDataOf(
                    "progress" to processed,
                    "max" to total
                ))
            }
            Log.i(TAG, "Historical sync complete — $count new transactions")
            Result.success(workDataOf(KEY_INSERTED_COUNT to count))
        } catch (e: Exception) {
            Log.e(TAG, "Historical sync failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG                = "SmsHistorySyncWorker"
        private const val KEY_LIMIT          = "limit"
        const val KEY_INSERTED_COUNT         = "inserted_count"
        private const val WORK_NAME          = "sms_history_sync"

        fun enqueue(context: Context, limit: Int = 200) {
            val request = OneTimeWorkRequestBuilder<SmsHistorySyncWorker>()
                .setInputData(workDataOf(KEY_LIMIT to limit))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,   // never run twice
                request
            )
        }
    }
}
