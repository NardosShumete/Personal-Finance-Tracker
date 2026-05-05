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
}
