package com.portfolio.financetracker.domain.repository

import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import kotlinx.coroutines.flow.Flow

interface CustomBankRepository {
    fun getAllCustomBanks(): Flow<List<CustomBankEntity>>
    fun getEnabledCustomBanks(): Flow<List<CustomBankEntity>>
    suspend fun getEnabledCustomBanksSync(): List<CustomBankEntity>
    suspend fun insertCustomBank(customBank: CustomBankEntity)
    suspend fun updateCustomBank(customBank: CustomBankEntity)
    suspend fun deleteCustomBank(customBank: CustomBankEntity)
}
