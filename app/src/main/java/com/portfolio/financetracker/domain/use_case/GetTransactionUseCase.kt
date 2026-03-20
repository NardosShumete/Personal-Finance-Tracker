package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository

class GetTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Int): Transaction? {
        return repository.getTransactionById(id)
    }
}
