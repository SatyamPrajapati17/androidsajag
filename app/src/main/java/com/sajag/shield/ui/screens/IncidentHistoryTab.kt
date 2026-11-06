package com.sajag.shield.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.domain.model.IncidentRecord
import com.sajag.shield.domain.model.ThreatLevel
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurface
import com.sajag.shield.ui.theme.CyberSurfaceBorder
import com.sajag.shield.ui.theme.CyberTextMuted
import com.sajag.shield.ui.theme.CyberTextPrimary
import com.sajag.shield.ui.theme.CyberTextSecondary
import com.sajag.shield.ui.theme.ThreatCriticalColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentHistoryTab(
    incidents: List<IncidentRecord>,
    isDownloadingPdf: Boolean,
    onDownloadPdf: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1930 CYBERCRIME DOSSIERS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Court-ready NCRP evidence documents & audit logs",
                            fontFamily = FontFamily.Default,
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${incidents.size} INCIDENTS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(incidents) { incident ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = incident.callerNumber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CyberTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(incident.timestamp)),
                                fontFamily = FontFamily.Default,
                                fontSize = 11.sp,
                                color = CyberTextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ThreatCriticalColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(incident.maxScamProb * 100).toInt()}% RISK",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = ThreatCriticalColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF07111D))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CATEGORY: ${incident.category}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF07111D))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val mins = incident.durationSeconds / 60
                            val secs = incident.durationSeconds % 60
                            Text(
                                text = "BAITED: ${mins}m ${secs}s",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberSafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // PDF Dossier action button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onDownloadPdf(incident.callId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF050B14)
                            )
                        ) {
                            if (isDownloadingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF050B14)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VIEW 1930 DOSSIER",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🚨 SAJAG CYBERCRIME ALERT (Helpline 1930)\n" +
                                        "Suspect Intercepted: ${incident.callerNumber}\n" +
                                        "Scam Probability: ${(incident.maxScamProb * 100).toInt()}%\n" +
                                        "Category: ${incident.category}\n" +
                                        "Mule Handles: ${incident.extractedIdentifiers.joinToString { it.value }}\n" +
                                        "Evidence Dossier: ${incident.pdfReportUrl}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share 1930 Alert via WhatsApp"))
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberSafeGreen
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberSafeGreen.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
