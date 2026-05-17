package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_table",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        // Fast lookup by source (MANUAL vs SMS)
        Index(value = ["source"]),
        // Unique constraint on smsHash prevents duplicate SMS inserts.
        // NULL values are excluded from uniqueness (MANUAL entries have null hash).
        Index(value = ["smsHash"], unique = true)
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
    val recurringPeriod: String = "NONE",

    // ── SMS Auto-Parse columns (nullable — safe for existing MANUAL rows) ─────
    val source: String = "MANUAL",
    val rawSms: String? = null,
    val smsBalance: Double? = null,
    val smsHash: String? = null,
    /** Content Provider _id — secondary dedup key for historical sync */
    val smsId: String? = null,
    /** True = waiting for user confirmation in the Pending Review screen */
    val isPending: Boolean = false,
    /** Exact bank name from parser — never guessed from category string */
    val bankName: String? = null
)
