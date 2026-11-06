package com.sajag.shield.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurface
import com.sajag.shield.ui.theme.CyberSurfaceBorder
import com.sajag.shield.ui.theme.CyberTextMuted
import com.sajag.shield.ui.theme.CyberTextPrimary
import java.io.File

@Composable
fun PdfViewerDialog(
    pdfFile: File,
    shareUri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember { mutableStateOf(1) }
    var pfd: ParcelFileDescriptor? by remember { mutableStateOf(null) }
    var renderer: PdfRenderer? by remember { mutableStateOf(null) }

    DisposableEffect(pdfFile) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            pfd = fileDescriptor
            renderer = pdfRenderer
            pageCount = pdfRenderer.pageCount

            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                // Render at 2x density for crisp legal document typography
                val width = page.width * 2
                val height = page.height * 2
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageBitmap = bmp
                page.close()
            }
        } catch (e: Exception) {
            Log.e("PdfViewerDialog", "Failed to render PDF using PdfRenderer: ${e.message}", e)
        }

        onDispose {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6050B14))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1930 NCRP CYBERCRIME DOSSIER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = pdfFile.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable rendered PDF Page
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    pageBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "1930 Incident Dossier Page",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rendering court-ready evidence document...",
                                color = Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1-Click WhatsApp & Legal Share Button
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🚨 OFFICIAL 1930 CYBERCRIME FIRST INCIDENT DOSSIER\n" +
                                "Generated by Sajag Mobile Shield (Autonomous Telephony Defence)\n" +
                                "Evidence admissibility compliant under Section 65B Indian Evidence Act."
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share 1930 Dossier via WhatsApp"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSafeGreen,
                        contentColor = Color(0xFF050B14)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-CLICK WHATSAPP & POLICE SHARE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
