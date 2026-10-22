package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisRed
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    EMERGENCY
}

@Composable
fun ArcReactorVisualizer(
    state: VisualizerState,
    rmsLevel: Float, // 0..10
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    // Slow rotation for ambient idle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == VisualizerState.PROCESSING) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation for counter ring
    val reverseRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Breathing pulse for core glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when (state) {
        VisualizerState.EMERGENCY -> JarvisRed
        VisualizerState.SPEAKING -> JarvisGold
        VisualizerState.PROCESSING -> JarvisCyanGlow
        VisualizerState.LISTENING -> JarvisCyan
        VisualizerState.IDLE -> JarvisCyan.copy(alpha = 0.7f)
    }

    val secondaryColor = when (state) {
        VisualizerState.EMERGENCY -> Color(0xFFFF8A80)
        VisualizerState.SPEAKING -> JarvisCyan
        else -> JarvisGold
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("arc_reactor_core"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val maxRadius = (this.size.minDimension / 2) - 8.dp.toPx()

            // 1. Ambient Background Glow
            val glowRadius = maxRadius * (if (state == VisualizerState.LISTENING) (1f + (rmsLevel / 20f)) else pulseScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (state == VisualizerState.LISTENING) 0.35f else 0.18f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Outer Segmented Ring
            val outerRadius = maxRadius * 0.92f
            val segments = 8
            val segmentAngle = 360f / segments
            for (i in 0 until segments) {
                val startAngle = (i * segmentAngle) + rotationAngle
                drawArc(
                    color = primaryColor.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = segmentAngle * 0.55f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Counter-Rotating Inner Dotted Ring
            val middleRadius = maxRadius * 0.74f
            drawCircle(
                color = secondaryColor.copy(alpha = 0.5f),
                radius = middleRadius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), reverseRotationAngle)
                )
            )

            // 4. Audio Waveform Bars (Active when LISTENING)
            val barCount = 24
            val baseBarRadius = maxRadius * 0.52f
            for (b in 0 until barCount) {
                val angle = (b * (360f / barCount)) * (Math.PI / 180f)
                val rmsModulation = if (state == VisualizerState.LISTENING) {
                    val variation = sin(b * 1.5 + rotationAngle / 20.0).toFloat()
                    (rmsLevel * 2.5f * (0.6f + 0.4f * variation)).coerceIn(2f, 28f)
                } else if (state == VisualizerState.PROCESSING) {
                    (sin(b * 0.8 + rotationAngle / 10.0) * 8f + 8f).toFloat()
                } else {
                    4f
                }

                val startR = baseBarRadius
                val endR = baseBarRadius + rmsModulation.dp.toPx()

                val p1 = Offset(
                    (center.x + startR * cos(angle)).toFloat(),
                    (center.y + startR * sin(angle)).toFloat()
                )
                val p2 = Offset(
                    (center.x + endR * cos(angle)).toFloat(),
                    (center.y + endR * sin(angle)).toFloat()
                )

                drawLine(
                    color = if (b % 3 == 0) secondaryColor else primaryColor,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Core Central Arc Reactor Chamber
            val coreRadius = maxRadius * 0.36f * (if (state == VisualizerState.LISTENING) (0.95f + (rmsLevel / 35f)) else pulseScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Core outline
            drawCircle(
                color = primaryColor,
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner triangular / hexagon cyber reticle
            val triangleRadius = coreRadius * 0.55f
            val points = (0 until 3).map { idx ->
                val a = (idx * 120f + rotationAngle * 1.5f) * (Math.PI / 180f)
                Offset(
                    (center.x + triangleRadius * cos(a)).toFloat(),
                    (center.y + triangleRadius * sin(a)).toFloat()
                )
            }
            drawLine(primaryColor, points[0], points[1], strokeWidth = 2.dp.toPx())
            drawLine(primaryColor, points[1], points[2], strokeWidth = 2.dp.toPx())
            drawLine(primaryColor, points[2], points[0], strokeWidth = 2.dp.toPx())
        }
    }
}
