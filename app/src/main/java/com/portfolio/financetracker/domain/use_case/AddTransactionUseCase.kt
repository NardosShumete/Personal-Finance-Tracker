package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    @Throws(InvalidTransactionException::class)
    suspend operator fun invoke(transaction: Transaction) {
        if (transaction.amount <= 0.0) {
            throw InvalidTransactionException("The amount must be greater than zero.")
        }
        if (transaction.category.isBlank()) {
            throw InvalidTransactionException("The category cannot be empty.")
        }
        repository.insertTransaction(transaction)
    }
}

class InvalidTransactionException(message: String): Exception(message)
