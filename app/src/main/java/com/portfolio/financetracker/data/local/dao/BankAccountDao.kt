package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_account_table")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_account_table WHERE bankName = :bankName")
    suspend fun getBankAccountByName(bankName: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bankAccount: BankAccountEntity)

    @Update
    suspend fun updateBankAccount(bankAccount: BankAccountEntity)
    
    @Query("UPDATE bank_account_table SET totalTransactions = totalTransactions + 1, lastKnownBalance = :newBalance, lastUpdated = :lastUpdated WHERE bankName = :bankName")
    suspend fun updateBalanceAndCount(bankName: String, newBalance: Double, lastUpdated: Long)
}
