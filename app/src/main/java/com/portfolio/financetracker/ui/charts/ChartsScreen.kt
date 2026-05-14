package com.portfolio.financetracker.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfolio.financetracker.ui.charts.components.GlowLineChart
import com.portfolio.financetracker.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Donut chart colors — vivid palette
    val chartColors = listOf(
        Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFFF43F5E), Color(0xFF06B6D4),
        Color(0xFFEC4899), Color(0xFF6366F1), Color(0xFFF97316)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient glow — dark mode only
        if (isSystemInDarkTheme()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GradientBlue.copy(alpha = 0.1f), Color.Transparent),
                            radius = 700f
                        )
                    )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(0.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Total expense header ──────────────────────────────────────
                GlassSection {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TOTAL EXPENSES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Br ${String.format("%.2f", state.totalExpense)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financeColors.expense
                        )
                    }
                }

                // ── Animated donut chart ──────────────────────────────────────
                if (state.categoryExpenses.isNotEmpty() && state.totalExpense > 0) {
                    GlassSection {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Expense Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Donut chart
                            GlowDonutChart(
                                categoryExpenses = state.categoryExpenses,
                                totalExpense     = state.totalExpense,
                                colors           = chartColors,
                                modifier         = Modifier
                                    .size(200.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Legend
                            state.categoryExpenses.entries
                                .sortedByDescending { it.value }
                                .forEachIndexed { index, entry ->
                                    val pct = (entry.value / state.totalExpense * 100).toInt()
                                    val color = chartColors[index % chartColors.size]
                                    LegendRow(
                                        label  = entry.key,
                                        amount = "Br ${String.format("%.2f", entry.value)}",
                                        pct    = "$pct%",
                                        color  = color
                                    )
                                }
                        }
                    }
                }

                // ── Spending trend line chart ─────────────────────────────────
                if (state.categoryExpenses.size >= 2) {
                    GlassSection {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Spending by Category",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlowLineChart(
                                dataPoints = state.categoryExpenses.values
                                    .map { it.toFloat() },
                                lineColor  = ElectricRose,
                                modifier   = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                // ── Income trend ──────────────────────────────────────────────
                GlassSection {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Balance Overview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlowLineChart(
                            dataPoints = listOf(
                                state.totalExpense.toFloat() * 0.3f,
                                state.totalExpense.toFloat() * 0.6f,
                                state.totalExpense.toFloat() * 0.45f,
                                state.totalExpense.toFloat() * 0.8f,
                                state.totalExpense.toFloat()
                            ),
                            lineColor  = EmeraldGreen,
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Donut chart ───────────────────────────────────────────────────────────────

@Composable
private fun GlowDonutChart(
    categoryExpenses: Map<String, Double>,
    totalExpense: Double,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryExpenses) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(1200))
    }

    val sweeps = categoryExpenses.values.map { (it / totalExpense * 360).toFloat() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val stroke = size.minDimension * 0.18f
            val inset  = stroke / 2f

            sweeps.forEachIndexed { i, sweep ->
                val animSweep = sweep * animProgress.value
                // Glow layer
                drawArc(
                    color      = colors[i % colors.size].copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = animSweep,
                    useCenter  = false,
                    style      = Stroke(width = stroke + 8.dp.toPx(), cap = StrokeCap.Round),
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke)
                )
                // Main arc
                drawArc(
                    color      = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = animSweep,
                    useCenter  = false,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke)
                )
                startAngle += sweep
            }
        }
        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Br ${String.format("%.0f", totalExpense)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Legend row ────────────────────────────────────────────────────────────────

@Composable
private fun LegendRow(label: String, amount: String, pct: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = pct,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Glass section wrapper ─────────────────────────────────────────────────────

@Composable
private fun GlassSection(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        content()
    }
}
