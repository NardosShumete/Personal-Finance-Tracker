package com.portfolio.financetracker.ui.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.financetracker.R
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.ui.dashboard.DashboardState
import com.portfolio.financetracker.ui.dashboard.SummaryPeriod
import com.portfolio.financetracker.ui.theme.*

@Composable
fun SummaryCard(
    state: DashboardState,
    onPeriodChange: (SummaryPeriod) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyCode = LocalCurrencyCode.current

    val meshGradient = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to GradientBlue,
            0.4f to GradientPurple,
            0.7f to GradientIndigo,
            1.0f to GradientTeal
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(meshGradient)
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        radius = 600f
                    )
                )
        )

        Column(modifier = Modifier.padding(24.dp)) {

            // ── Period toggle ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                SummaryPeriod.entries.forEach { period ->
                    val isSelected = state.selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.20f)
                                else Color.Transparent
                            )
                            .clickable { onPeriodChange(period) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (period) {
                                SummaryPeriod.TODAY      -> "Today"
                                SummaryPeriod.THIS_MONTH -> "Month"
                                SummaryPeriod.ALL_TIME   -> "All"
                            },
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Label ─────────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.total_balance).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── Hero balance (always all-time net) ────────────────────────────
            Text(
                text = CurrencyHelper.formatAmount(state.totalBalance, currencyCode),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Income / Expense row (period-filtered) ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalancePill(
                    label    = stringResource(R.string.income),
                    amount   = CurrencyHelper.formatAmount(state.displayIncome, currencyCode),
                    color    = EmeraldGreen,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                BalancePill(
                    label    = stringResource(R.string.expense),
                    amount   = CurrencyHelper.formatAmount(state.displayExpense, currencyCode),
                    color    = ElectricRose,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Budget progress (uses month expense vs goal) ───────────────────
            state.monthlyGoal?.let { goal ->
                if (goal.expenseLimit > 0) {
                    Spacer(modifier = Modifier.height(20.dp))

                    val rawProgress = (state.monthExpense / goal.expenseLimit).toFloat()
                        .coerceIn(0f, 1f)

                    val animatedProgress by animateFloatAsState(
                        targetValue   = rawProgress,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label         = "budget_progress"
                    )

                    val barColor = when {
                        rawProgress > 0.9f -> ElectricRose
                        rawProgress > 0.7f -> Color(0xFFF59E0B)
                        else               -> EmeraldGreen
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = stringResource(R.string.budget_usage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text       = "${(rawProgress * 100).toInt()}%",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = barColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(barColor, barColor.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val remaining = (goal.expenseLimit - state.monthExpense).coerceAtLeast(0.0)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text  = "${CurrencyHelper.formatAmount(state.monthExpense, currencyCode)} ${stringResource(R.string.spent)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text  = "${CurrencyHelper.formatAmount(remaining, currencyCode)} ${stringResource(R.string.left)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancePill(
    label: String,
    amount: String,
    color: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
