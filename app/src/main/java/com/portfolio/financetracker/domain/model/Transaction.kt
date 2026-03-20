package com.portfolio.financetracker.domain.model

enum class RecurringPeriod {
    NONE, WEEKLY, MONTHLY
}

data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: TransactionType,
    val note: String,
    val receiptPath: String? = null,
    val recurringPeriod: RecurringPeriod = RecurringPeriod.NONE
)
