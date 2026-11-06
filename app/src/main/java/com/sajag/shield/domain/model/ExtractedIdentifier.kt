package com.sajag.shield.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedIdentifier(
    @SerialName("call_id")
    val callId: String? = null,
    
    @SerialName("turn_seq")
    val turnSeq: Int? = null,
    
    @SerialName("type")
    val type: String, // "upi", "org_claimed", "phone", "account", "bank"
    
    @SerialName("value")
    val value: String,
    
    @SerialName("normalized")
    val normalized: String? = null,
    
    @SerialName("syntax_valid")
    val syntaxValid: Boolean = true,
    
    @SerialName("quote")
    val quote: String? = null,
    
    @SerialName("confidence")
    val confidence: Float? = 1.0f,
    
    @SerialName("source")
    val source: String? = "caller_stated",
    
    @SerialName("verified")
    val verified: Boolean? = false,
    
    @SerialName("superseded")
    val superseded: Boolean? = false,
    
    @SerialName("created_at")
    val createdAt: String? = null
)
