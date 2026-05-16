package com.portfolio.financetracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 5 → 6: adds SMS columns to transaction_table
 * (source, rawSms, smsBalance, smsHash, smsId, isPending)
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
        // custom_bank_table — user-defined bank configurations
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

        // bank_account_table — per-bank balance tracking
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bank_account_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bankName` TEXT NOT NULL,
                `senderAddress` TEXT NOT NULL,
                `lastKnownBalance` REAL NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                `totalTransactions` INTEGER NOT NULL,
                `colorHex` TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_bank_account_table_bankName`
            ON `bank_account_table` (`bankName`)
            """.trimIndent()
        )
    }
}

/**
 * Migration 7 → 8: adds bankName column to transaction_table + reminder_table.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Fix CBE/BOA merge bug — store exact bank name from parser
        db.execSQL("ALTER TABLE `transaction_table` ADD COLUMN `bankName` TEXT")

        // reminder_table — calendar reminders and scheduled transactions
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `date` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'General',
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `repeatInterval` TEXT NOT NULL DEFAULT 'NONE',
                `autoGenerateExpense` INTEGER NOT NULL DEFAULT 1,
                `syncToGoogleCalendar` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}
