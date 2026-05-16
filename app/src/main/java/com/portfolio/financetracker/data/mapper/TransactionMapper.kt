package com.portfolio.financetracker.data.mapper

import com.portfolio.financetracker.data.local.entity.TransactionEntity
import com.portfolio.financetracker.domain.model.RecurringPeriod
import com.portfolio.financetracker.domain.model.Transaction
import com.portfolio.financetracker.domain.model.TransactionSource
import com.portfolio.financetracker.domain.model.TransactionType

fun TransactionEntity.toDomainModel(): Transaction = Transaction(
    id              = id,
    amount          = amount,
    category        = category,
    date            = date,
    type            = TransactionType.valueOf(type),
    note            = note,
    receiptPath     = receiptPath,
    recurringPeriod = RecurringPeriod.valueOf(recurringPeriod),
    source          = runCatching { TransactionSource.valueOf(source) }
                          .getOrDefault(TransactionSource.MANUAL),
    rawSms          = rawSms,
    smsBalance      = smsBalance,
    smsHash         = smsHash,
    smsId           = smsId,
    isPending       = isPending,
    bankName        = bankName
)

fun Transaction.toEntityModel(): TransactionEntity = TransactionEntity(
    id              = id,
    amount          = amount,
    category        = category,
    date            = date,
    type            = type.name,
    note            = note,
    receiptPath     = receiptPath,
    recurringPeriod = recurringPeriod.name,
    source          = source.name,
    rawSms          = rawSms,
    smsBalance      = smsBalance,
    smsHash         = smsHash,
    smsId           = smsId,
    isPending       = isPending,
    bankName        = bankName
)
