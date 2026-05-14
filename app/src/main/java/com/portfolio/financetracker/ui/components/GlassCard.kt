package com.portfolio.financetracker.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.portfolio.financetracker.ui.theme.MidnightBorder

/**
 * Frosted-glass card effect.
 *
 * Compose does not expose a native RenderEffect blur on all API levels,
 * so we simulate glassmorphism with:
 *   - Semi-transparent dark surface (0.7 alpha)
 *   - Subtle white border (10% opacity)
 *   - Rounded corners
 *
 * On API 31+ you can layer a BlurMaskFilter via a Canvas modifier for
 * a true backdrop blur — this implementation is compatible with API 26+.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    glassColor: Color = Color(0xFF1E293B).copy(alpha = 0.75f),
    borderColor: Color = MidnightBorder,
    borderWidth: Dp = 0.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape),
        shape = shape,
        color = glassColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(content = content)
    }
}
