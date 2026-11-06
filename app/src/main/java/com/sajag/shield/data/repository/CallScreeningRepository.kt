package com.sajag.shield.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.sajag.shield.BuildConfig
import com.sajag.shield.data.remote.N8nWebhookApi
import com.sajag.shield.data.remote.SajagBackendApi
import com.sajag.shield.domain.model.ExtractedIdentifier
import com.sajag.shield.domain.model.N8nEventPayload
import com.sajag.shield.domain.model.TurnRequest
import com.sajag.shield.domain.model.TurnResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallScreeningRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendApi: SajagBackendApi,
    private val n8nApi: N8nWebhookApi
) {
    private val tag = "SajagRepository"

    private val upiPattern = Pattern.compile(
        "\\b[a-zA-Z0-9.\\-_]{2,64}@[a-zA-Z]{2,32}\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val orgPattern = Pattern.compile(
        "\\b(CBI|Customs|Narcotics Control Bureau|NCB|Police|Cyber Crime|ED|Enforcement Directorate|RBI|Income Tax)\\b",
        Pattern.CASE_INSENSITIVE
    )

    suspend fun checkContactWhitelist(rawNumber: String): Pair<Boolean, String?> =
        withContext(Dispatchers.IO) {
            val cleanDigits = rawNumber.replace(Regex("[^0-9]"), "")
            val familyDigits = BuildConfig.FAMILY_ALERT_NUMBER.replace(Regex("[^0-9]"), "")
            if (cleanDigits.isNotEmpty() && familyDigits.isNotEmpty() &&
                (cleanDigits.endsWith(familyDigits) || familyDigits.endsWith(cleanDigits))) {
                Log.d(tag, "Whitelist match: $rawNumber is registered Family Contact")
                return@withContext Pair(true, "Family Contact (${BuildConfig.FAMILY_ALERT_NUMBER})")
            }

            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission || rawNumber.isBlank()) {
                return@withContext Pair(false, null)
            }

            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(rawNumber)
                )
                val projection = arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Known Contact"
                        Log.d(tag, "Whitelist match: $rawNumber is '$name'")
                        return@withContext Pair(true, name)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to query ContactsContract: ${e.message}")
            }

            Pair(false, null)
        }

    suspend fun submitTurn(
        callId: String,
        callerNumber: String,
        text: String
    ): Result<TurnResponse> = withContext(Dispatchers.IO) {
        try {
            val request = TurnRequest(
                callId = callId,
                callerNumber = callerNumber,
                speaker = "scammer",
                text = text
            )
            Log.d(tag, "Streaming turn to backend: $request")
            val response = backendApi.sendTurn(request)
            Log.d(tag, "Backend response received: p_scam=${response.pScam}, level=${response.level}, handover=${response.shouldHandover}")
            Result.success(response)
        } catch (e: Exception) {
            Log.w(tag, "Backend call failed (${e.message}), executing local fallback intelligence")
            val fallbackResponse = buildLocalIntelligenceFallback(callId, callerNumber, text)
            Result.success(fallbackResponse)
        }
    }

    suspend fun dispatchN8nAlert(
        callId: String,
        callerNumber: String,
        pScam: Float,
        category: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = N8nEventPayload(
                callId = callId,
                callerNumber = callerNumber,
                pScam = pScam,
                category = category,
                targetFamilyPhone = BuildConfig.FAMILY_ALERT_NUMBER
            )
            Log.d(tag, "Dispatching n8n event to ${BuildConfig.N8N_WEBHOOK_URL}: $payload")
            val response = n8nApi.sendEvent(
                url = BuildConfig.N8N_WEBHOOK_URL,
                secret = BuildConfig.N8N_SECRET_KEY,
                payload = payload
            )
            val success = response.isSuccessful
            Log.d(tag, "n8n response code: ${response.code()} success=$success")
            Result.success(success)
        } catch (e: Exception) {
            Log.w(tag, "n8n dispatch failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun buildLocalIntelligenceFallback(
        callId: String,
        callerNumber: String,
        text: String
    ): TurnResponse {
        val lower = text.lowercase()
        val hasDigitalArrest = lower.contains("digital arrest") || lower.contains("arrest warrant")
        val hasAuthority = lower.contains("cbi") ||
                lower.contains("police") ||
                lower.contains("inspector") ||
                lower.contains("customs") ||
                lower.contains("crime branch") ||
                lower.contains("cyber crime") ||
                lower.contains("narcotics") ||
                lower.contains("ncb") ||
                lower.contains("enforcement directorate") ||
                lower.contains("ed") ||
                lower.contains("rbi") ||
                lower.contains("court") ||
                lower.contains("commissioner") ||
                lower.contains("dcp") ||
                lower.contains("acp")

        val hasThreat = lower.contains("parcel") ||
                lower.contains("drugs") ||
                lower.contains("raid") ||
                lower.contains("seized") ||
                lower.contains("fir") ||
                lower.contains("jail") ||
                lower.contains("block") ||
                lower.contains("freeze") ||
                lower.contains("immediately")

        val hasDemand = lower.contains("money") ||
                lower.contains("transfer") ||
                lower.contains("pay") ||
                lower.contains("fine") ||
                lower.contains("penalty") ||
                lower.contains("deposit") ||
                lower.contains("upi") ||
                lower.contains("police.verify") ||
                lower.contains("account") ||
                lower.contains("bank") ||
                lower.contains("rs") ||
                lower.contains("rupees") ||
                lower.contains("charges") ||
                lower.contains("settlement")

        val signals = mutableListOf<String>()
        if (hasAuthority) signals.add("authority")
        if (hasThreat) signals.add("threat")
        if (hasDemand) signals.add("demand")
        if (hasDigitalArrest) signals.add("urgency")

        // Auto-detect high scam probability if unknown caller impersonates authority and asks for money
        var score = when {
            hasDigitalArrest -> 0.98f
            hasAuthority && hasDemand -> 0.95f
            hasAuthority && hasThreat -> 0.89f
            hasDemand && hasThreat -> 0.75f
            hasAuthority -> 0.45f
            hasDemand -> 0.40f
            else -> 0.10f
        }

        score = score.coerceIn(0.0f, 1.0f)
        val shouldHandover = score >= 0.88f
        val level = when {
            score >= 0.88f -> "CRITICAL"
            score >= 0.70f -> "HIGH"
            score >= 0.40f -> "MEDIUM"
            else -> "LOW"
        }

        // Extract UPIs
        val extracted = mutableListOf<ExtractedIdentifier>()
        val upiMatcher = upiPattern.matcher(text)
        while (upiMatcher.find()) {
            val handle = upiMatcher.group()
            extracted.add(
                ExtractedIdentifier(
                    callId = callId,
                    turnSeq = 1,
                    type = "upi",
                    value = handle,
                    normalized = handle.lowercase(),
                    syntaxValid = true,
                    quote = handle,
                    confidence = 1.0f,
                    source = "on_device_extractor"
                )
            )
        }

        val orgMatcher = orgPattern.matcher(text)
        if (orgMatcher.find()) {
            val org = orgMatcher.group()
            extracted.add(
                ExtractedIdentifier(
                    callId = callId,
                    turnSeq = 1,
                    type = "org_claimed",
                    value = org,
                    normalized = org.uppercase(),
                    syntaxValid = true,
                    quote = org,
                    confidence = 0.95f,
                    source = "on_device_extractor"
                )
            )
        }

        return TurnResponse(
            callId = callId,
            pScam = score,
            level = level,
            mode = if (shouldHandover) "PENDING" else "MONITOR",
            category = if (hasDigitalArrest) "DIGITAL_ARREST" else if (hasAuthority) "POLICE_IMPERSONATION" else "BENIGN",
            signals = signals,
            shouldHandover = shouldHandover,
            handoverAudioUrl = if (shouldHandover) "${BuildConfig.BACKEND_BASE_URL}audio/ramesh_voice.mp3" else null,
            decoyReply = if (shouldHandover) "Hello? Haan ji boliye, kaun bol rahe hain? Mujhe theek se sunai nahi de raha..." else null,
            retellAgentId = BuildConfig.RETELL_AGENT_ID,
            extractedIdentifiers = extracted,
            safetyBlocks = 0
        )
    }
}
