package com.portfolio.financetracker.domain.model

import kotlinx.serialization.Serializable

/**
 * Tracks how a transaction entered the system.
 *
 * MANUAL  — user typed it in the Add Transaction screen
 * SMS     — auto-parsed from an incoming bank SMS
 *
 * Stored as a String in Room so future sources (e.g. CSV_IMPORT, API)
 * can be added without a schema migration.
 */
@Serializable
enum class TransactionSource {
    MANUAL,
    SMS
}
