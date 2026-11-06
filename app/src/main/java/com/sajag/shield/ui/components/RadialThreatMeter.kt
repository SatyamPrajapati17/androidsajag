package com.sajag.shield.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.domain.model.ThreatLevel
import com.sajag.shield.ui.theme.CyberBackground
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.ThreatCriticalColor
import com.sajag.shield.ui.theme.ThreatHighColor
import com.sajag.shield.ui.theme.ThreatLowColor
import com.sajag.shield.ui.theme.ThreatMediumColor

@Composable
fun RadialThreatMeter(
    probability: Float,
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val animatedProb by animateFloatAsState(
        targetValue = probability.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "probAnim"
    )

    val targetColor = when (threatLevel) {
        ThreatLevel.CRITICAL -> ThreatCriticalColor
        ThreatLevel.HIGH -> ThreatHighColor
        ThreatLevel.MEDIUM -> ThreatMediumColor
        ThreatLevel.LOW -> ThreatLowColor
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "colorAnim"
    )

    // Pulsing glow when threat is high or critical
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (probability >= 0.70f) 0.85f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 16.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Background arc (240 degrees sweep, starting from 150 deg)
            val startAngle = 150f
            val sweepMax = 240f

            drawArc(
                color = Color(0xFF132238),
                startAngle = startAngle,
                sweepAngle = sweepMax,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Threat Arc
            val currentSweep = sweepMax * animatedProb
            if (currentSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to ThreatLowColor,
                        0.4f to ThreatMediumColor,
                        0.7f to ThreatHighColor,
                        1.0f to ThreatCriticalColor
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Radar Tick Marks
            for (i in 0..12) {
                val tickAngle = Math.toRadians((startAngle + (sweepMax / 12f) * i).toDouble())
                val innerR = radius - strokeWidth * 0.9f
                val outerR = radius - strokeWidth * 0.4f
                val startX = center.x + (innerR * Math.cos(tickAngle)).toFloat()
                val startY = center.y + (innerR * Math.sin(tickAngle)).toFloat()
                val endX = center.x + (outerR * Math.cos(tickAngle)).toFloat()
                val endY = center.y + (outerR * Math.sin(tickAngle)).toFloat()

                drawLine(
                    color = if (i <= (12 * animatedProb).toInt()) animatedColor else Color(0xFF263C5A),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Outer Pulse Ring when critical
            if (animatedProb >= 0.88f) {
                drawCircle(
                    color = ThreatCriticalColor.copy(alpha = pulseAlpha * 0.4f),
                    radius = radius + strokeWidth * 0.7f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(animatedProb * 100).toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                color = animatedColor
            )
            Text(
                text = threatLevel.label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = animatedColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (animatedProb >= 0.88f) "HANDOVER ARMED" else "AI GUARDIAN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                color = if (animatedProb >= 0.88f) ThreatCriticalColor else CyberCyan
            )
        }
    }
}
