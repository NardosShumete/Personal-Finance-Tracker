package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_table",
    indices = [
        // Speeds up ORDER BY date DESC (default sort for all queries)
        Index(value = ["date"]),
        // Speeds up WHERE category = ? (search/filter queries)
        Index(value = ["category"]),
        // Speeds up WHERE type = ? (income/expense split queries)
        Index(value = ["type"])
    ]
)
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
