package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.portfolio.financetracker.data.local.entity.CustomBankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBankDao {
    @Query("SELECT * FROM custom_bank_table")
    fun getAllCustomBanks(): Flow<List<CustomBankEntity>>

    @Query("SELECT * FROM custom_bank_table WHERE isEnabled = 1")
    fun getEnabledCustomBanks(): Flow<List<CustomBankEntity>>
    
    @Query("SELECT * FROM custom_bank_table WHERE isEnabled = 1")
    suspend fun getEnabledCustomBanksSync(): List<CustomBankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomBank(customBank: CustomBankEntity)

    @Update
    suspend fun updateCustomBank(customBank: CustomBankEntity)

    @Delete
    suspend fun deleteCustomBank(customBank: CustomBankEntity)
}
