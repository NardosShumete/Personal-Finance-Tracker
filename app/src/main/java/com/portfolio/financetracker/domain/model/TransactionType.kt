package com.portfolio.financetracker.domain.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
enum class TransactionType {
    INCOME, EXPENSE
}
