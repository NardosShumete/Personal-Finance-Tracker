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
    val transactionCount: Int = 0,
    /**
     * The most recent balance reported in an SMS for this bank.
     * This is the actual account balance as reported by the bank,
     * NOT income minus expense. Null until the first SMS is confirmed.
     */
    val lastKnownBalance: Double? = null
)
