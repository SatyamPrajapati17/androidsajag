package com.sajag.shield.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class N8nEventPayload(
    @SerialName("call_id")
    val callId: String,
    
    @SerialName("caller_number")
    val callerNumber: String,
    
    @SerialName("p_scam")
    val pScam: Float,
    
    @SerialName("category")
    val category: String,
    
    @SerialName("target_family_phone")
    val targetFamilyPhone: String = "+919922996010",
    
    @SerialName("event")
    val event: String = "DECOY_TAKEOVER",
    
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
