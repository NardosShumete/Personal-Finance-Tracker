package com.portfolio.financetracker.domain.repository

import androidx.paging.PagingData
import com.portfolio.financetracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    /**
     * Returns a [Flow] of [PagingData] for the transaction list.
     * Used by the dashboard to load transactions incrementally.
     */
    fun getPagedTransactions(): Flow<PagingData<Transaction>>

    /**
     * Returns all transactions as a plain list.
     * Kept for summary calculations (total income / expense / balance)
     * where we need the full dataset, not a paged subset.
     */
    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    suspend fun getTransactionById(id: Int): Transaction?

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transaction: Transaction)

    /**
     * Inserts a transaction parsed from SMS only if no transaction with the
     * same [Transaction.smsHash] already exists. Returns true if inserted,
     * false if it was a duplicate.
     */
    suspend fun insertFromSmsIfNotDuplicate(transaction: Transaction): Boolean

    /** Returns pending (unconfirmed) SMS transactions as a live Flow */
    fun getPendingTransactions(): Flow<List<Transaction>>

    /** Live count of pending transactions for UI badge */
    fun getPendingCount(): Flow<Int>

    /** Marks a transaction as confirmed (isPending = false) */
    suspend fun confirmTransaction(id: Int)

    /**
     * Syncs historical SMS from the device inbox.
     * Reads up to [limitPerSender] messages per bank, parses them,
     * and inserts only new ones (deduped by smsId + smsHash).
     * Returns the count of newly inserted transactions.
     */
    suspend fun syncSmsHistory(context: android.content.Context, limitPerSender: Int = 200): Int
}
