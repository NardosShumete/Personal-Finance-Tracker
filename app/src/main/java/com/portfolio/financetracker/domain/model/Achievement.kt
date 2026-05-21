package com.portfolio.financetracker.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Emoji or icon name
    val isUnlocked: Boolean = false,
    val progress: Float = 0f
)
