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

    @Query("SELECT * FROM bank_account_table WHERE shortName = :shortName")
    suspend fun getBankAccountByShortName(shortName: String): BankAccountEntity?

    @Query("SELECT * FROM bank_account_table WHERE smsSenderId = :senderId")
    suspend fun getBankAccountBySenderId(senderId: String): BankAccountEntity?

    @Query("UPDATE bank_account_table SET totalIncome = totalIncome + :income, totalExpense = totalExpense + :expense, transactionCount = transactionCount + 1 WHERE shortName = :shortName")
    suspend fun updateTotals(shortName: String, income: Double, expense: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bankAccount: BankAccountEntity)

    @Update
    suspend fun updateBankAccount(bankAccount: BankAccountEntity)

    @Query("UPDATE bank_account_table SET totalIncome = :income, totalExpense = :expense, transactionCount = :count WHERE id = :id")
    suspend fun updateTotals(id: Int, income: Double, expense: Double, count: Int)
}
