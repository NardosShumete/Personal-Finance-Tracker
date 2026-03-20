package com.portfolio.financetracker.data.repository

import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.data.mapper.toEntityModel
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> {
        return dao.getTransactionsByType(type).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction.toEntityModel())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction.toEntityModel())
    }
}
