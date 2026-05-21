package com.portfolio.financetracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 5 → 6: adds SMS columns to transaction_table
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `rawSms` TEXT")
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `smsBalance` REAL")
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `smsHash` TEXT")
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `smsId` TEXT")
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `isPending` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_table_source` ON `transaction_table` (`source`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_table_smsHash` ON `transaction_table` (`smsHash`)")
    }
}

/**
 * Migration 6 → 7: adds custom_bank_table and bank_account_table
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_bank_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `senderAddress` TEXT NOT NULL,
                `creditKeyword` TEXT NOT NULL,
                `debitKeyword` TEXT NOT NULL,
                `balanceKeyword` TEXT NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                `colorHex` TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bank_account_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shortName` TEXT NOT NULL,
                `fullName` TEXT NOT NULL,
                `smsSenderId` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isConnected` INTEGER NOT NULL,
                `totalIncome` REAL NOT NULL,
                `totalExpense` REAL NOT NULL,
                `transactionCount` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_bank_account_table_shortName`
            ON `bank_account_table` (`shortName`)
            """.trimIndent()
        )
    }
}

/**
 * Migration 7 → 8: adds bankName column to transaction_table + reminder_table.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `bankName` TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `date` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `repeatInterval` TEXT NOT NULL,
                `autoGenerateExpense` INTEGER NOT NULL,
                `syncToGoogleCalendar` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Migration 8 → 9: adds receiptPath to transaction_table
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `receiptPath` TEXT")
    }
}

/**
 * Migration 9 → 10: adds recurringPeriod to transaction_table
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `recurringPeriod` TEXT NOT NULL DEFAULT 'NONE'")
    }
}

/**
 * Migration 10 → 11: adds indices for performance
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_table_date` ON `transaction_table` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_table_category` ON `transaction_table` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_table_type` ON `transaction_table` (`type`)")
    }
}

/**
 * Migration 11 → 12: Empty migration to resolve integrity hash mismatch after branch merge
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes, just forcing a version bump to refresh Room's identity hash
    }
}

/**
 * Migration 12 → 13: Force refresh identity hash
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes, just forcing a version bump to refresh Room's identity hash
    }
}

/**
 * Migration 13 → 14:
 * 1. Adds lastKnownBalance column to bank_account_table (the actual balance
 *    reported by the bank in the SMS, not income-minus-expense).
 * 2. Removes case-duplicate bank rows (e.g. "telebirr" vs "Telebirr") by
 *    keeping only the row with the highest id (most recently inserted).
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the new column — nullable so existing rows default to NULL
        db.execSQL(
            "ALTER TABLE `bank_account_table` ADD COLUMN `lastKnownBalance` REAL"
        )

        // Remove case-insensitive duplicates: for each group of rows that share
        // the same shortName (case-insensitive), keep only the one with the
        // highest id and delete the rest.
        db.execSQL(
            """
            DELETE FROM bank_account_table
            WHERE id NOT IN (
                SELECT MAX(id)
                FROM bank_account_table
                GROUP BY LOWER(shortName)
            )
            """.trimIndent()
        )

        // Normalise remaining shortNames to their canonical casing
        // (update any lowercase "telebirr" → "Telebirr", etc.)
        val canonicalNames = mapOf(
            "cbe"      to "CBE",
            "boa"      to "BOA",
            "telebirr" to "Telebirr",
            "hibret"   to "Hibret",
            "dashen"   to "Dashen",
            "awash"    to "Awash"
        )
        canonicalNames.forEach { (lower, canonical) ->
            db.execSQL(
                "UPDATE bank_account_table SET shortName = '$canonical' WHERE LOWER(shortName) = '$lower'"
            )
        }
    }
}
