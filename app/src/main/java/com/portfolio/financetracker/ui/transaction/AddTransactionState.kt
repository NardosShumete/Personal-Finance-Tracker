package com.portfolio.financetracker.ui.transaction

import com.portfolio.financetracker.domain.model.TransactionType

data class AddTransactionState(
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val date: Long = System.currentTimeMillis()
)
