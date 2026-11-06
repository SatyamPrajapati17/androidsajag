package com.sajag.shield.domain.model

data class IncidentRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val callId: String,
    val callerNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val maxScamProb: Float,
    val threatLevel: ThreatLevel,
    val category: String,
    val extractedIdentifiers: List<ExtractedIdentifier> = emptyList(),
    val pdfReportUrl: String,
    val pdfDownloaded: Boolean = false,
    val localPdfPath: String? = null
)
