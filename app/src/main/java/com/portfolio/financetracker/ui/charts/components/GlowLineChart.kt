package com.portfolio.financetracker.ui.charts.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.financetracker.ui.theme.EmeraldGreen
import com.portfolio.financetracker.ui.theme.ElectricRose
import com.portfolio.financetracker.ui.theme.Slate400

/**
 * Smooth curved line chart with:
 * - Cubic Bezier curves (cubicTo) for fluid lines
 * - Glowing gradient fill underneath the line
 * - Animated draw-on effect
 * - Dot markers at data points
 */
@Composable
fun GlowLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = EmeraldGreen,
    label: String = ""
) {
    if (dataPoints.size < 2) {
        Text(
            text = "Not enough data",
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )
        return
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }

    if (label.isNotBlank()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = lineColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16.dp.toPx()

        val minVal = dataPoints.min()
        val maxVal = dataPoints.max()
        val range  = (maxVal - minVal).takeIf { it > 0f } ?: 1f

        // Map data → canvas coordinates
        fun xOf(i: Int) = padding + i * (w - 2 * padding) / (dataPoints.size - 1)
        fun yOf(v: Float) = h - padding - ((v - minVal) / range) * (h - 2 * padding)

        val points = dataPoints.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }

        // ── Build smooth cubic Bezier path ────────────────────────────────────
        val linePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cpX = (p0.x + p1.x) / 2f
                cubicTo(cpX, p0.y, cpX, p1.y, p1.x, p1.y)
            }
        }

        // ── Clip to animated progress (left → right reveal) ───────────────────
        val clipWidth = w * animProgress.value
        clipRect(right = clipWidth) {

            // ── Gradient fill under the line ──────────────────────────────────
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // ── Glow layer (wider, lower opacity) ─────────────────────────────
            drawPath(
                path = linePath,
                color = lineColor.copy(alpha = 0.25f),
                style = Stroke(
                    width = 12.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // ── Main line ─────────────────────────────────────────────────────
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // ── Dot markers ───────────────────────────────────────────────────
            points.forEach { pt ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}
