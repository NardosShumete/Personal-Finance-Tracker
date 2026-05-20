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

    @Query("SELECT * FROM transaction_table ORDER BY date DESC")
    fun getTransactionsPaged(): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transaction_table ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_table WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transaction_table WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transaction_table WHERE smsHash = :hash")
    suspend fun countBySmsHash(hash: String): Int

    @Query("SELECT * FROM transaction_table WHERE source = 'SMS' ORDER BY date DESC")
    fun getSmsTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_table WHERE isPending = 1 ORDER BY date DESC")
    fun getPendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transaction_table WHERE isPending = 1")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE transaction_table SET isPending = 0 WHERE id = :id")
    suspend fun confirmTransaction(id: Int)

    @Query("SELECT COUNT(*) FROM transaction_table WHERE smsId = :smsId")
    suspend fun countBySmsId(smsId: String): Int

    @Query("SELECT * FROM transaction_table WHERE bankName = :bankName AND isPending = 0 ORDER BY date DESC")
    fun getTransactionsByBank(bankName: String): Flow<List<TransactionEntity>>
}
