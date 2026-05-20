package com.portfolio.financetracker.domain.manager

import com.portfolio.financetracker.domain.model.Achievement
import com.portfolio.financetracker.domain.model.SavingsGoal
import com.portfolio.financetracker.domain.model.SavingsGoalStatus

object AchievementManager {
    fun calculateAchievements(goals: List<SavingsGoal>): List<Achievement> {
        val completedCount = goals.count { it.status == SavingsGoalStatus.COMPLETED }
        val totalSaved = goals.sumOf { it.currentAmount }
        
        return listOf(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Create your first savings goal",
                icon = "🌱",
                isUnlocked = goals.isNotEmpty(),
                progress = if (goals.isNotEmpty()) 1f else 0f
            ),
            Achievement(
                id = "saver_beginner",
                title = "Saver Beginner",
                description = "Complete 1 savings goal",
                icon = "💰",
                isUnlocked = completedCount >= 1,
                progress = (completedCount / 1f).coerceAtMost(1f)
            ),
            Achievement(
                id = "halfway_there",
                title = "Halfway Hero",
                description = "Reach 50% of any goal",
                icon = "🚀",
                isUnlocked = goals.any { it.progress >= 0.5f },
                progress = if (goals.any { it.progress >= 0.5f }) 1f else 0f
            ),
            Achievement(
                id = "big_saver",
                title = "Big Saver",
                description = "Save a total of 10,000",
                icon = "🏆",
                isUnlocked = totalSaved >= 10000.0,
                progress = (totalSaved / 10000.0).toFloat().coerceAtMost(1f)
            )
        )
    }
}
