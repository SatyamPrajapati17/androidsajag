package com.sajag.shield.domain.model

data class TranscriptTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: String, // "scammer", "user", "decoy"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isScamIndicator: Boolean = false
)

data class CallSession(
    val callId: String = "mob-" + System.currentTimeMillis(),
    val callerNumber: String = "+919876543210",
    val isKnownContact: Boolean = false,
    val contactName: String? = null,
    val startTimeMs: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val pScam: Float = 0.0f,
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val status: ShieldStatus = ShieldStatus.IDLE,
    val isMicMuted: Boolean = false,
    val isHandoverComplete: Boolean = false,
    val transcripts: List<TranscriptTurn> = emptyList(),
    val extractedLeads: List<ExtractedIdentifier> = emptyList(),
    val signals: List<String> = emptyList(),
    val decoyReply: String? = null,
    val handoverAudioUrl: String? = null,
    val category: String? = null
)
