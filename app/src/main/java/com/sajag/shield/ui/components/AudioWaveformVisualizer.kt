package com.sajag.shield.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.ThreatCriticalColor

@Composable
fun AudioWaveformVisualizer(
    amplitudes: List<Float>,
    isDecoyActive: Boolean,
    isCritical: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        isCritical -> ThreatCriticalColor
        isDecoyActive -> CyberSafeGreen
        else -> CyberCyan
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayBars = amplitudes.take(28).ifEmpty { List(28) { 0.1f } }

        displayBars.forEachIndexed { index, amp ->
            val animatedHeight by animateFloatAsState(
                targetValue = (amp * 48f).coerceIn(4f, 52f),
                animationSpec = tween(durationMillis = 90),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                barColor,
                                barColor.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
        }
    }
}
