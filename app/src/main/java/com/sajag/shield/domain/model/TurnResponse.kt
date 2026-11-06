package com.sajag.shield.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TurnResponse(
    @SerialName("call_id")
    val callId: String,
    
    @SerialName("p_scam")
    val pScam: Float,
    
    @SerialName("level")
    val level: String,
    
    @SerialName("mode")
    val mode: String? = null,
    
    @SerialName("category")
    val category: String? = null,
    
    @SerialName("signals")
    val signals: List<String> = emptyList(),
    
    @SerialName("should_handover")
    val shouldHandover: Boolean = false,
    
    @SerialName("handover_audio_url")
    val handoverAudioUrl: String? = null,
    
    @SerialName("decoy_reply")
    val decoyReply: String? = null,
    
    @SerialName("retell_agent_id")
    val retellAgentId: String? = null,
    
    @SerialName("extracted_identifiers")
    val extractedIdentifiers: List<ExtractedIdentifier> = emptyList(),
    
    @SerialName("safety_blocks")
    val safetyBlocks: Int = 0
)
