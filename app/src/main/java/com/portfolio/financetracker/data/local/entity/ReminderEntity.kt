package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_table")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val type: String, // "DEPOSIT", "WITHDRAW", "RENT", "UTILITY", "SUBSCRIPTION"
    val category: String = "General",
    val isCompleted: Boolean = false,
    val repeatInterval: String = "NONE", // "NONE", "WEEKLY", "MONTHLY"
    val autoGenerateExpense: Boolean = true,
    val syncToGoogleCalendar: Boolean = false
)
