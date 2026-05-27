package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke() {
        repository.deleteAllTransactions()
    }
}
