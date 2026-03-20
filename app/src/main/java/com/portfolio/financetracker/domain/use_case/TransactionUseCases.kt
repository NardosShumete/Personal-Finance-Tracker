package com.portfolio.financetracker.domain.use_case

data class TransactionUseCases(
    val getTransactions: GetTransactionsUseCase,
    val getTransaction: GetTransactionUseCase,
    val deleteTransaction: DeleteTransactionUseCase,
    val addTransaction: AddTransactionUseCase
)
