package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_bank_table")
data class CustomBankEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val senderAddress: String,
    val creditKeyword: String,
    val debitKeyword: String,
    val balanceKeyword: String,
    val isEnabled: Boolean,
    val colorHex: String
)
