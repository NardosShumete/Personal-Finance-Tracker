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
