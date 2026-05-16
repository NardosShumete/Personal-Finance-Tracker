package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>
    suspend fun getBankAccountByShortName(shortName: String): BankAccountEntity?
    suspend fun getBankAccountBySenderId(senderId: String): BankAccountEntity?
    suspend fun insertBankAccount(bankAccount: BankAccountEntity)
    suspend fun updateBankAccount(bankAccount: BankAccountEntity)
    suspend fun updateTotals(id: Int, income: Double, expense: Double, count: Int)
}
