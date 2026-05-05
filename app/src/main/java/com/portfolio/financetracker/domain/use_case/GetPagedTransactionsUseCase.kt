package com.portfolio.financetracker.domain.use_case

import androidx.paging.PagingData
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that exposes the paged transaction stream to the ViewModel.
 *
 * Keeping this as a dedicated use case maintains Clean Architecture:
 * the ViewModel never touches the Repository directly.
 */
class GetPagedTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<PagingData<Transaction>> =
        repository.getPagedTransactions()
}
