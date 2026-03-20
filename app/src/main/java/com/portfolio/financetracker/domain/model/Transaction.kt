package com.portfolio.financetracker.domain.model

data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: TransactionType,
    val note: String
)
