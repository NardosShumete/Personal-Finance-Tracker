package com.portfolio.financetracker.domain.use_case

import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPendingCountUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<Int> {
        return repository.getPendingCount()
    }
}
