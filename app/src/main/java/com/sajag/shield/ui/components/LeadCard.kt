package com.sajag.shield.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sajag.shield.domain.model.ExtractedIdentifier
import com.sajag.shield.ui.theme.CyberCyan
import com.sajag.shield.ui.theme.CyberSafeGreen
import com.sajag.shield.ui.theme.CyberSurface
import com.sajag.shield.ui.theme.CyberSurfaceBorder
import com.sajag.shield.ui.theme.CyberTextMuted
import com.sajag.shield.ui.theme.CyberTextPrimary
import com.sajag.shield.ui.theme.CyberTextSecondary
import com.sajag.shield.ui.theme.ThreatCriticalColor

@Composable
fun LeadCard(
    lead: ExtractedIdentifier,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val leadIcon = when (lead.type.lowercase()) {
        "upi" -> Icons.Default.AccountBalance
        "phone" -> Icons.Default.Phone
        else -> Icons.Default.Security
    }

    val typeColor = when (lead.type.lowercase()) {
        "upi" -> CyberCyan
        "phone" -> ThreatCriticalColor
        else -> Color(0xFFFFB800)
    }

    Box(
        modifier = modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = leadIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lead.type.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = typeColor
                    )
                }

                // Syntax valid badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSafeGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyberSafeGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lead.syntaxValid) "VALID SYNTAX" else "SUSPECT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = CyberSafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value + Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lead.value,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(lead.value))
                        Toast.makeText(context, "Copied: ${lead.value}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy lead",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Transcript Grounding Quote
            lead.quote?.let { quote ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF07111D))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "GROUNDING TRANSCRIPT EVIDENCE:",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = CyberTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"$quote\"",
                            fontFamily = FontFamily.Default,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = CyberTextSecondary
                        )
                    }
                }
            }
        }
    }
}
