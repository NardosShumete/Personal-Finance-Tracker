package com.portfolio.financetracker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfolio.financetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ── Paging 3 ─────────────────────────────────────────────────────────────
    // Room generates the PagingSource implementation automatically.
    // Results are ordered by the indexed `date` column for fast retrieval.
    @Query("SELECT * FROM transaction_table ORDER BY date DESC")
    fun getTransactionsPaged(): PagingSource<Int, TransactionEntity>

    // ── Existing queries (kept for summary calculations) ──────────────────────
    @Query("SELECT * FROM transaction_table ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_table WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    // Task 3 — IGNORE conflict strategy: if smsHash unique index fires,
    // the insert is silently skipped instead of throwing an exception.
    // This is the database-level safety net (application-level check is first).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Uses the `type` index for fast filtering
    @Query("SELECT * FROM transaction_table WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    // ── SMS deduplication ─────────────────────────────────────────────────────
    /**
     * Returns true if a transaction with this hash already exists.
     * Called before inserting an SMS-parsed transaction to prevent duplicates.
     */
    @Query("SELECT COUNT(*) FROM transaction_table WHERE smsHash = :hash")
    suspend fun countBySmsHash(hash: String): Int

    /** Returns all SMS-sourced transactions for audit/review UI */
    @Query("SELECT * FROM transaction_table WHERE source = 'SMS' ORDER BY date DESC")
    fun getSmsTransactions(): Flow<List<TransactionEntity>>

    // ── Pending review ────────────────────────────────────────────────────────

    /** All transactions awaiting user confirmation */
    @Query("SELECT * FROM transaction_table WHERE isPending = 1 ORDER BY date DESC")
    fun getPendingTransactions(): Flow<List<TransactionEntity>>

    /** Count of pending transactions — used for badge on nav item */
    @Query("SELECT COUNT(*) FROM transaction_table WHERE isPending = 1")
    fun getPendingCount(): Flow<Int>

    /** Confirm a pending transaction (set isPending = false) */
    @Query("UPDATE transaction_table SET isPending = 0 WHERE id = :id")
    suspend fun confirmTransaction(id: Int)

    /** Secondary dedup check using Content Provider smsId */
    @Query("SELECT COUNT(*) FROM transaction_table WHERE smsId = :smsId")
    suspend fun countBySmsId(smsId: String): Int
}
