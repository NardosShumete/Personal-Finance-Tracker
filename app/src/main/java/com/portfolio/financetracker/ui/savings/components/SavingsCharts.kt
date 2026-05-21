package com.portfolio.financetracker.ui.savings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.financetracker.domain.model.SavingsGoal

@Composable
fun SavingsPieChart(
    goals: List<SavingsGoal>,
    modifier: Modifier = Modifier
) {
    val categoryTotals = goals.groupBy { it.category }
        .mapValues { it.value.sumOf { goal -> goal.currentAmount } }
        .filter { it.value > 0 }

    val totalSaved = categoryTotals.values.sum()
    
    if (totalSaved == 0.0) return

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            categoryTotals.forEach { (category, amount) ->
                val sweepAngle = (amount / totalSaved * 360).toFloat()
                val goalForColor = goals.find { it.category == category }
                val color = goalForColor?.let { Color(it.colorHex) } ?: Color.Gray
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 40f)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Allocation",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${categoryTotals.size} Categories",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
