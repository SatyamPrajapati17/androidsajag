package com.sajag.shield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.ui.theme.CyberBackground
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurface
import com.sajag.shield.ui.theme.CyberSurfaceBorder
import com.sajag.shield.ui.theme.CyberTextMuted
import com.sajag.shield.ui.theme.CyberTextPrimary
import com.sajag.shield.ui.viewmodel.ShieldViewModel

@Composable
fun MainShieldScreen(
    viewModel: ShieldViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.session.collectAsState()
    val waveformAmplitudes by viewModel.waveformAmplitudes.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val viewingPdfFile by viewModel.viewingPdfFile.collectAsState()
    val isDownloadingPdf by viewModel.isDownloadingPdf.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield Logo",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAJAG MOBILE SHIELD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "Track 5: CYBER-01 • Autonomous Scam Interception",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = CyberTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "1930 HELPLINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = CyberCyan
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, CyberSurfaceBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val items = listOf(
                    Triple("LIVE HUD", Icons.Default.PhoneInTalk, 0),
                    Triple("SUSPECT LEADS", Icons.Default.Fingerprint, 1),
                    Triple("1930 HISTORY", Icons.Default.Description, 2)
                )

                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f),
                            unselectedIconColor = CyberTextMuted,
                            unselectedTextColor = CyberTextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LiveHudTab(
                    session = session,
                    waveformAmplitudes = waveformAmplitudes,
                    onPanicClicked = { viewModel.forceHandoverToRameshBot() },
                    onSimulateScam = { viewModel.simulateScamCallScenario("DIGITAL_ARREST") },
                    onSimulateNormal = { viewModel.simulateNormalCall() },
                    onEndCall = { viewModel.endCall() },
                    onSendTurn = { viewModel.sendCustomTurn(it) }
                )
                1 -> SuspectLeadsTab(
                    extractedLeads = session.extractedLeads,
                    callId = session.callId
                )
                2 -> IncidentHistoryTab(
                    incidents = incidents,
                    isDownloadingPdf = isDownloadingPdf,
                    onDownloadPdf = { viewModel.downloadAndOpenPdf(it) }
                )
            }

            // PDF Viewer Modal Dialog
            viewingPdfFile?.let { pdfFile ->
                val shareUri = viewModel.getShareablePdfUri(pdfFile)
                PdfViewerDialog(
                    pdfFile = pdfFile,
                    shareUri = shareUri,
                    onDismiss = { viewModel.closePdfViewer() }
                )
            }
        }
    }
}
