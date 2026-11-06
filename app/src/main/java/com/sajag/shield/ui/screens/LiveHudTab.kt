package com.sajag.shield.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.domain.model.CallSession
import com.sajag.shield.domain.model.ShieldStatus
import com.sajag.shield.domain.model.ThreatLevel
import com.sajag.shield.ui.components.AudioWaveformVisualizer
import com.sajag.shield.ui.components.HandoverAlertBanner
import com.sajag.shield.ui.components.RadialThreatMeter
import com.sajag.shield.ui.components.StatusBadge
import com.sajag.shield.ui.theme.CyberAlertRed
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurface
import com.sajag.shield.ui.theme.CyberSurfaceBorder
import com.sajag.shield.ui.theme.CyberTextMuted
import com.sajag.shield.ui.theme.CyberTextPrimary
import com.sajag.shield.ui.theme.CyberTextSecondary
import com.sajag.shield.ui.theme.ThreatCriticalColor

@Composable
fun LiveHudTab(
    session: CallSession,
    waveformAmplitudes: List<Float>,
    onPanicClicked: () -> Unit,
    onSimulateScam: () -> Unit,
    onSimulateNormal: () -> Unit,
    onEndCall: () -> Unit,
    onSendTurn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var customInput by remember { mutableStateOf("") }

    LaunchedEffect(session.transcripts.size) {
        if (session.transcripts.isNotEmpty()) {
            listState.animateScrollToItem(session.transcripts.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            StatusBadge(status = session.status)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active Telephony Details Pill
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CALLER TELEPHONY LINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = CyberTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.contactName ?: session.callerNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (session.isKnownContact) CyberSafeGreen else CyberCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (session.isKnownContact) CyberSafeGreen.copy(alpha = 0.15f) else CyberAlertRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (session.isKnownContact) "SAFE (KNOWN CONTACT)" else "UNKNOWN NUMBER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (session.isKnownContact) CyberSafeGreen else CyberAlertRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dedicated Safe Contact Whitelist Banner
        if (session.isKnownContact) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSafeGreen.copy(alpha = 0.12f))
                        .border(1.dp, CyberSafeGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberSafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SAFE CALL — KNOWN CONTACT VERIFIED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberSafeGreen
                            )
                            Text(
                                text = "Scam detection and decoy takeover bypassed. Your voice and call are 100% private.",
                                fontFamily = FontFamily.Default,
                                fontSize = 10.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Radial Threat Gauge
        item {
            RadialThreatMeter(
                probability = session.pScam,
                threatLevel = session.threatLevel
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live Audio Waveform
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE SPEECH AUDIO DOWNLINK (16kHz PCM)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = CyberTextMuted
                    )
                    Text(
                        text = if (session.status == ShieldStatus.DECOY_ACTIVE) "RAMESH VOICE INJECTED" else "LISTENING",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (session.status == ShieldStatus.DECOY_ACTIVE) CyberAlertRed else CyberCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                AudioWaveformVisualizer(
                    amplitudes = waveformAmplitudes,
                    isDecoyActive = session.status == ShieldStatus.DECOY_ACTIVE,
                    isCritical = session.threatLevel == ThreatLevel.CRITICAL
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Full Screen Warning Banner if Scammer Detected
        item {
            HandoverAlertBanner(
                visible = session.status == ShieldStatus.DECOY_ACTIVE || session.threatLevel == ThreatLevel.CRITICAL,
                callerNumber = session.callerNumber,
                decoyReply = session.decoyReply
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Panic Button: Force Handover to Ramesh Bot
        item {
            Button(
                onClick = onPanicClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberAlertRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "🚨 FORCE HANDOVER TO RAMESH BOT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Simulator / Live Testing Console
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF091424))
                    .border(1.dp, Color(0xFF1B3150), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "🧪 ON-DEVICE SIMULATOR (TEST SCENARIOS)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSimulateScam,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C1016),
                            contentColor = CyberAlertRed
                        )
                    ) {
                        Text(
                            text = "Simulate Scam",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onSimulateNormal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D251D),
                            contentColor = CyberSafeGreen
                        )
                    ) {
                        Text(
                            text = "Simulate Safe",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onEndCall,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2838),
                            contentColor = CyberTextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom turn input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Type or speak custom scammer speech...",
                                fontSize = 11.sp,
                                color = CyberTextMuted
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            color = CyberTextPrimary,
                            fontFamily = FontFamily.Default
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberSurfaceBorder,
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (customInput.isNotBlank()) {
                                onSendTurn(customInput)
                                customInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Turn",
                            tint = Color(0xFF050B14),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Live Transcripts Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME TRANSCRIPT FEED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberTextMuted
                )
                Text(
                    text = "${session.transcripts.size} TURNS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = CyberCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Transcript Items
        items(session.transcripts) { turn ->
            val speakerColor = when (turn.speaker.lowercase()) {
                "scammer" -> ThreatCriticalColor
                "decoy" -> CyberCyan
                "user" -> CyberSafeGreen
                else -> CyberTextSecondary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (turn.speaker == "decoy") CyberCyan.copy(alpha = 0.5f) else CyberSurfaceBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = turn.speaker.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = speakerColor
                        )
                        Text(
                            text = "TURN GROUNDED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = CyberTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = turn.text,
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = CyberTextPrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
