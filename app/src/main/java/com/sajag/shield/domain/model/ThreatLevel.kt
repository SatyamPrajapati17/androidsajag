package com.sajag.shield.domain.model

import androidx.compose.ui.graphics.Color

enum class ThreatLevel(val label: String, val threshold: Float) {
    LOW("LOW RISK", 0.0f),
    MEDIUM("SUSPICIOUS", 0.40f),
    HIGH("HIGH THREAT", 0.70f),
    CRITICAL("CRITICAL SCAM", 0.88f);

    companion object {
        fun fromProbability(p: Float): ThreatLevel {
            return when {
                p >= 0.88f -> CRITICAL
                p >= 0.70f -> HIGH
                p >= 0.40f -> MEDIUM
                else -> LOW
            }
        }

        fun fromBackendString(level: String?): ThreatLevel {
            return when (level?.uppercase()) {
                "CRITICAL" -> CRITICAL
                "HIGH" -> HIGH
                "MEDIUM" -> MEDIUM
                else -> LOW
            }
        }
    }
}

enum class ShieldStatus(val displayName: String) {
    MONITORING("MONITORING"),
    PENDING_TAKEOVER("PENDING TAKEOVER"),
    DECOY_ACTIVE("DECOY ACTIVE"),
    IDLE("SHIELD STANDBY"),
    CALL_ENDED("CALL CONCLUDED"),
    SAFE_WHITELIST("SAFE (KNOWN CONTACT)")
}
