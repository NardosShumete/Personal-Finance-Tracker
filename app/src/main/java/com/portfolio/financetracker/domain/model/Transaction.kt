package com.portfolio.financetracker.domain.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
enum class RecurringPeriod {
    NONE, WEEKLY, MONTHLY
}

@OptIn(InternalSerializationApi::class)
@Serializable
data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: TransactionType,
    val note: String,
    val receiptPath: String? = null,
    val recurringPeriod: RecurringPeriod = RecurringPeriod.NONE,

    // ── SMS Auto-Parse fields ─────────────────────────────────────────────────
    /** Where this transaction came from — MANUAL or SMS */
    val source: TransactionSource = TransactionSource.MANUAL,
    /** The raw SMS body — stored for audit/debugging, never shown in main UI */
    val rawSms: String? = null,
    /** Running balance reported in the SMS (e.g. "Available balance: 5,200.00") */
    val smsBalance: Double? = null,
    /**
     * Deduplication key — SHA-256 hash of (sender + amount + date-minute + type).
     * Prevents the same SMS from being inserted twice if the receiver fires twice.
     * Null for MANUAL entries.
     */
    val smsHash: String? = null,
    /**
     * Content Provider _id from the SMS inbox.
     * Used as a secondary deduplication key during historical sync.
     * Null for real-time SMS (hash is used instead) and MANUAL entries.
     */
    val smsId: String? = null,
    /**
     * True when this transaction is waiting for user confirmation.
     * SMS-parsed transactions start as pending; user confirms or edits them.
     */
    val isPending: Boolean = false,
    /**
     * The exact bank name from the parser (e.g. "CBE", "Abyssinia", "Telebirr").
     * Stored directly so we never have to guess it from the category string.
     * Null for MANUAL entries.
     */
    val bankName: String? = null
)
