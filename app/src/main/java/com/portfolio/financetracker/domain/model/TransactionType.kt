package com.portfolio.financetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    INCOME, EXPENSE
}
