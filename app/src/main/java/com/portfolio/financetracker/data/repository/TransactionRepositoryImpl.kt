package com.portfolio.financetracker.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.portfolio.financetracker.core.sms.SmsInboxReader
import com.portfolio.financetracker.core.sms.SmsParser
import com.portfolio.financetracker.data.local.DataStoreManager
import com.portfolio.financetracker.data.local.dao.BankAccountDao
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import com.portfolio.financetracker.data.local.paging.TransactionPagingSource
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.data.mapper.toEntityModel
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val bankAccountDao: BankAccountDao,
    private val dataStoreManager: DataStoreManager
) : TransactionRepository {

    /**
     * Paged transaction stream.
     *
     * [PagingConfig] tuning:
     * - pageSize = 20       → load 20 rows at a time (good for a finance list)
     * - prefetchDistance = 5 → start loading the next page when 5 items remain
     * - enablePlaceholders = false → simpler UI; no empty placeholder items
     *
     * [flowOn(Dispatchers.IO)] ensures all DB reads happen off the main thread.
     */
    override fun getPagedTransactions(): Flow<PagingData<Transaction>> =
        Pager(
            config = PagingConfig(
                pageSize           = 20,
                prefetchDistance   = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { TransactionPagingSource(dao) }
        ).flow.flowOn(Dispatchers.IO)

    /**
     * Full list — used only for summary totals (income / expense / balance).
     * [flowOn(Dispatchers.IO)] keeps DB work off the main thread.
     */
    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        dao.getTransactionsByType(type)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    override suspend fun getTransactionById(id: Int): Transaction? =
        dao.getTransactionById(id)?.toDomainModel()

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction.toEntityModel())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction.toEntityModel())
    }

    /**
     * Inserts an SMS-parsed transaction only if no row with the same
     * [Transaction.smsHash] already exists in the database.
     *
     * This is the primary deduplication guard — the unique index on
     * `smsHash` in the entity is a secondary safety net.
     */
    override suspend fun insertFromSmsIfNotDuplicate(transaction: Transaction): Boolean {
        val hash = transaction.smsHash ?: return false
        val exists = dao.countBySmsHash(hash) > 0
        if (exists) return false
        dao.insertTransaction(transaction.toEntityModel())
        return true
    }

    override fun getPendingTransactions(): Flow<List<Transaction>> =
        dao.getPendingTransactions()
            .map { it.map { e -> e.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    override fun getPendingCount(): Flow<Int> =
        dao.getPendingCount()

    override suspend fun confirmTransaction(id: Int) {
        val transaction = dao.getTransactionById(id) ?: return
        dao.confirmTransaction(id)
        
        if (transaction.source == "SMS" && transaction.smsBalance != null) {
            val bankName = transaction.category.substringBefore(" Transfer").trim()
            if (bankName.isNotEmpty()) {
                val account = bankAccountDao.getBankAccountByName(bankName)
                if (account == null) {
                    bankAccountDao.insertBankAccount(
                        BankAccountEntity(
                            bankName = bankName,
                            senderAddress = "UNKNOWN", // Fallback if not tracked
                            lastKnownBalance = transaction.smsBalance,
                            lastUpdated = transaction.date,
                            totalTransactions = 1,
                            colorHex = "#10B981" // Default accent color
                        )
                    )
                } else {
                    bankAccountDao.updateBalanceAndCount(bankName, transaction.smsBalance, transaction.date)
                }
            }
        }
    }

    /**
     * Historical SMS sync — "Day 1 balance" feature.
     *
     * Algorithm:
     * 1. Read up to [limitPerSender] messages per bank from content://sms/inbox
     * 2. For each message, check smsId first (fastest), then smsHash
     * 3. Parse with [SmsParser] — skip if not a bank transaction
     * 4. Insert as isPending=true so user can review before it hits totals
     */
    override suspend fun syncSmsHistory(
        context: Context, 
        limitPerSender: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): Int {
        val trackedSenders = dataStoreManager.trackedSmsSenders.first()
        val rawMessages = SmsInboxReader.readFromTrackedSenders(context, trackedSenders, limitPerSender)
        
        // Process oldest first
        val sortedMessages = rawMessages.sortedBy { it.timestampMs }
        val total = sortedMessages.size
        
        var insertedCount = 0
        var processedCount = 0

        for (batch in sortedMessages.chunked(50)) {
            for (raw in batch) {
                processedCount++
                if (raw.smsId.isNotBlank() && dao.countBySmsId(raw.smsId) > 0) continue
                val parsed = SmsParser.parse(raw.sender, raw.body, raw.timestampMs, trackedSenders) ?: continue
                if (dao.countBySmsHash(parsed.hash) > 0) continue

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
                    smsId      = raw.smsId,
                    isPending  = true
                )
                dao.insertTransaction(transaction.toEntityModel())
                insertedCount++
            }
            onProgress(processedCount, total)
        }
        return insertedCount
    }
}
