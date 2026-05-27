package com.portfolio.financetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "failed_parse_table")
data class FailedParseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sender: String,
    val rawBody: String,
    val date: Long,
    val reason: String
)
