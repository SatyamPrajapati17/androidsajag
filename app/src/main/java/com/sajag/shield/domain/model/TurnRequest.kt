package com.sajag.shield.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TurnRequest(
    @SerialName("call_id")
    val callId: String,
    
    @SerialName("caller_number")
    val callerNumber: String? = null,
    
    @SerialName("speaker")
    val speaker: String = "scammer",
    
    @SerialName("text")
    val text: String
)
