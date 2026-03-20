package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.TransactionEntity
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionType

fun TransactionEntity.toDomainModel(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        category = category,
        date = date,
        type = TransactionType.valueOf(type),
        note = note
    )
}

fun Transaction.toEntityModel(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        category = category,
        date = date,
        type = type.name, // Convert enum to string
        note = note
    )
}
