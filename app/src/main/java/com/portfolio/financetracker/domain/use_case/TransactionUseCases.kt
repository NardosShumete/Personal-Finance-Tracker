package com.portfolio.financetracker.domain.use_case

data class TransactionUseCases(
    val getTransactions: GetTransactionsUseCase,
    val getPagedTransactions: GetPagedTransactionsUseCase,
    val getTransaction: GetTransactionUseCase,
    val deleteTransaction: DeleteTransactionUseCase,
    val deleteAllTransactions: DeleteAllTransactionsUseCase,
    val addTransaction: AddTransactionUseCase,
    val getPendingCount: GetPendingCountUseCase
)
