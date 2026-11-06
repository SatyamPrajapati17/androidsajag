package com.sajag.shield.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.domain.model.ShieldStatus
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurfaceVariant
import com.sajag.shield.ui.theme.ThreatCriticalColor
import com.sajag.shield.ui.theme.ThreatMediumColor

@Composable
fun StatusBadge(
    status: ShieldStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, badgeLabel) = when (status) {
        ShieldStatus.MONITORING -> CyberCyan to "MONITORING"
        ShieldStatus.PENDING_TAKEOVER -> ThreatMediumColor to "PENDING TAKEOVER"
        ShieldStatus.DECOY_ACTIVE -> ThreatCriticalColor to "DECOY ACTIVE"
        ShieldStatus.SAFE_WHITELIST -> CyberSafeGreen to "SAFE (KNOWN CONTACT)"
        ShieldStatus.IDLE -> CyberSafeGreen to "SHIELD STANDBY"
        ShieldStatus.CALL_ENDED -> Color(0xFF8B949E) to "CALL CONCLUDED"
    }

    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(400),
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceVariant.copy(alpha = 0.85f))
            .border(1.dp, animatedColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = if (status != ShieldStatus.IDLE) pulseAlpha else 0.8f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badgeLabel,
                color = animatedColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
