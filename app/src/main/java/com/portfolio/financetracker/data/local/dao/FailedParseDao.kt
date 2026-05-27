package com.portfolio.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.portfolio.financetracker.data.local.entity.FailedParseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FailedParseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFailedParse(failedParse: FailedParseEntity)

    @Query("SELECT * FROM failed_parse_table ORDER BY date DESC")
    fun getAllFailedParses(): Flow<List<FailedParseEntity>>

    @Query("DELETE FROM failed_parse_table")
    suspend fun deleteAllFailedParses()
}
