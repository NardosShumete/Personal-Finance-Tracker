package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_account_table",
    indices = [Index(value = ["bankName"], unique = true)]
)
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bankName: String,
    val senderAddress: String,
    val lastKnownBalance: Double,
    val lastUpdated: Long,
    val totalTransactions: Int,
    val colorHex: String
)
