package com.portfolio.financetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiTransaction(
    val amount: Double,
    val category: String,
    val type: String,
    val date: Long,
    val note: String
)
