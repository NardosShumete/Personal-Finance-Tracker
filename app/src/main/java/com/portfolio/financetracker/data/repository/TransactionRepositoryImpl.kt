package com.portfolio.financetracker.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.paging.TransactionPagingSource
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.data.mapper.toEntityModel
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
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
}
