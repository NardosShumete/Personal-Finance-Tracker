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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Uses the `type` index for fast filtering
    @Query("SELECT * FROM transaction_table WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
}
