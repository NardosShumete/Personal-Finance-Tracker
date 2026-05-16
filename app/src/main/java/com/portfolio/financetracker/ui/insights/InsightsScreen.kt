package com.portfolio.financetracker.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.core.util.CurrencyHelper
import com.portfolio.financetracker.core.util.LocalCurrencyCode
import com.portfolio.financetracker.domain.model.FinancialInsight
import com.portfolio.financetracker.domain.model.InsightPriority
import com.portfolio.financetracker.domain.model.InsightsData
import com.portfolio.financetracker.ui.charts.components.AnimatedPieChart
import com.portfolio.financetracker.ui.charts.components.GlowLineChart
import com.portfolio.financetracker.ui.components.GlassCard
import com.portfolio.financetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SlateWhite)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Midnight,
                    titleContentColor = SlateWhite
                )
            )
        },
        containerColor = Midnight
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldGreen)
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = ElectricRose)
            }
        } else {
            state.insightsData?.let { data ->
                InsightsContent(data, Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun InsightsContent(data: InsightsData, modifier: Modifier = Modifier) {
    val currencyCode = LocalCurrencyCode.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Summary Cards Row
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    label = "Net Savings",
                    amount = data.netSavings,
                    currencyCode = currencyCode,
                    icon = Icons.Default.Savings,
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Daily Avg",
                    amount = data.dailyAverage,
                    currencyCode = currencyCode,
                    icon = Icons.Default.TrendingDown,
                    color = ElectricRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Budget Usage
        item {
            BudgetWidget(data.budgetUsage, data.remainingBudget, currencyCode)
        }

        // 3. Alerts Section (if any)
        if (data.alerts.isNotEmpty()) {
            item {
                SectionHeader("Alerts")
            }
            items(data.alerts) { alert ->
                InsightCard(alert)
            }
        }

        // 4. Prediction Card
        item {
            PredictionCard(data.predictedBurnRate, currencyCode)
        }

        // 5. Category Analysis
        item {
            SectionHeader("Spending by Category")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    AnimatedPieChart(
                        categoryExpenses = data.categoryWiseExpenses,
                        totalExpense = data.totalExpenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }
        }

        // 6. Weekly Trends
        item {
            SectionHeader("Weekly Spending")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    val dataPoints = if (data.weeklySpending.isEmpty()) {
                        listOf(0f, 0f)
                    } else {
                        val maxWeek = data.weeklySpending.keys.maxOrNull() ?: 1
                        (1..maxWeek).map { data.weeklySpending[it]?.toFloat() ?: 0f }
                    }
                    
                    GlowLineChart(
                        dataPoints = dataPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        // 7. Human Insights
        if (data.humanReadableInsights.isNotEmpty()) {
            item {
                SectionHeader("Smart Insights")
            }
            items(data.humanReadableInsights) { insight ->
                InsightCard(insight)
            }
        }
    }
}

@Composable
fun SummaryCard(
    label: String, 
    amount: Double, 
    currencyCode: String,
    icon: ImageVector, 
    color: Color, 
    modifier: Modifier = Modifier
) {
    GlassCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Slate400, fontSize = 12.sp)
            Text(
                CurrencyHelper.formatAmount(amount, currencyCode), 
                color = SlateWhite, 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun BudgetWidget(usage: Float, remaining: Double, currencyCode: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = usage.coerceIn(0f, 1f),
        label = "budget_progress"
    )

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Budget", color = SlateWhite, fontWeight = FontWeight.Medium)
                Text("${(usage * 100).toInt()}% used", color = if (usage > 0.9) ElectricRose else EmeraldGreen)
            }
            Spacer(Modifier.height(12.dp))
            
            // Custom progress bar to match user's style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MidnightSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                if (usage > 0.9f) listOf(ElectricRose, RoseLight)
                                else listOf(EmeraldGreen, EmeraldLight)
                            )
                        )
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "Remaining: ${CurrencyHelper.formatAmount(remaining, currencyCode)}", 
                color = Slate400, 
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PredictionCard(burnRate: Double, currencyCode: String) {
    GlassCard(
        Modifier.fillMaxWidth(),
        glassColor = GradientIndigo.copy(alpha = 0.1f)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(GradientIndigo.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = GradientIndigo)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Predicted Monthly Spend", color = Slate400, fontSize = 12.sp)
                Text(
                    CurrencyHelper.formatAmount(burnRate, currencyCode), 
                    color = SlateWhite, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 20.sp
                )
                Text("Based on your current burn rate", color = Slate600, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun InsightCard(insight: FinancialInsight) {
    val (bgColor, icon, iconColor) = when (insight.priority) {
        InsightPriority.WARNING -> Triple(RoseBg, Icons.Default.Warning, ElectricRose)
        InsightPriority.SUCCESS -> Triple(EmeraldBg, Icons.Default.CheckCircle, EmeraldGreen)
        InsightPriority.INFO -> Triple(MidnightSurface, Icons.Default.Info, GradientBlue)
    }

    GlassCard(
        Modifier.fillMaxWidth(),
        glassColor = bgColor.copy(alpha = 0.4f)
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(insight.title, color = SlateWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(insight.message, color = Slate200, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = SlateWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}
