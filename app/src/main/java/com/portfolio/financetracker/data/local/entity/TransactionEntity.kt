package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_table")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: String,
    val note: String,
    val receiptPath: String? = null,
    val recurringPeriod: String = "NONE"
)
