package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_account_table",
    indices = [Index(value = ["shortName"], unique = true)]
)
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shortName: String,
    val fullName: String,
    val smsSenderId: String,
    val colorHex: String,
    val isConnected: Boolean,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactionCount: Int = 0
)
