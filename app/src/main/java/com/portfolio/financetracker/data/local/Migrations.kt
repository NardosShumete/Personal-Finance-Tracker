package com.portfolio.financetracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create custom_bank_table
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
        // Create bank_account_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bank_account_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `bankName` TEXT NOT NULL, 
                `senderAddress` TEXT NOT NULL, 
                `lastKnownBalance` REAL NOT NULL, 
                `lastUpdated` INTEGER NOT NULL, 
                `totalTransactions` INTEGER NOT NULL
                `colorHex` TEXT NOT NULL
            )
            """.trimIndent()
        )
        // Create index for bank_account_table
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_bank_account_table_bankName` 
            ON `bank_account_table` (`bankName`)
            """.trimIndent()
        )
    }
}
