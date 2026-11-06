package com.sajag.shield.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sajag.shield.data.repository.CallScreeningRepository
import com.sajag.shield.data.repository.IncidentRepository
import com.sajag.shield.domain.model.CallSession
import com.sajag.shield.domain.model.IncidentRecord
import com.sajag.shield.domain.model.ShieldStatus
import com.sajag.shield.domain.model.ThreatLevel
import com.sajag.shield.domain.model.TranscriptTurn
import com.sajag.shield.domain.model.TurnResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CallScreeningRepository,
    private val incidentRepository: IncidentRepository,
    private val decoyAudioPlayer: DecoyAudioPlayer
) {
    private val tag = "CallStateManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _session = MutableStateFlow(CallSession())
    val session: StateFlow<CallSession> = _session.asStateFlow()

    private val _waveformAmplitudes = MutableStateFlow(List(32) { 0.15f })
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

    private val _handoverEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val handoverEvents: SharedFlow<String> = _handoverEvents.asSharedFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var micMuteWatchdogJob: Job? = null

    fun startCall(callerNumber: String, isKnown: Boolean, contactName: String?) {
        val initialStatus = if (isKnown) ShieldStatus.SAFE_WHITELIST else ShieldStatus.MONITORING
        val initialPScam = 0.0f
        val initialTurn = if (isKnown) {
            TranscriptTurn(
                speaker = "system",
                text = "🟢 SAFE CALL: $callerNumber verified in Contacts Whitelist (${contactName ?: "Known Contact"}). Scam detection bypassed for family/friend."
            )
        } else {
            TranscriptTurn(
                speaker = "system",
                text = "🟡 UNKNOWN NUMBER: Monitoring $callerNumber for Police/CBI/Demand scam signatures."
            )
        }

        val newSession = CallSession(
            callId = "mob-" + System.currentTimeMillis(),
            callerNumber = callerNumber,
            isKnownContact = isKnown,
            contactName = contactName,
            startTimeMs = System.currentTimeMillis(),
            status = initialStatus,
            pScam = initialPScam,
            threatLevel = ThreatLevel.LOW,
            isMicMuted = false,
            isHandoverComplete = false,
            transcripts = listOf(initialTurn)
        )
        _session.value = newSession
        Log.d(tag, "Started new session: ${newSession.callId} for $callerNumber (isKnown=$isKnown)")
    }

    fun addTranscriptTurn(speaker: String, text: String) {
        val current = _session.value

        // Known numbers are strictly safe — NEVER evaluate as scam
        if (current.isKnownContact) {
            val safeTurn = TranscriptTurn(
                speaker = speaker,
                text = text,
                isScamIndicator = false
            )
            _session.value = current.copy(
                transcripts = current.transcripts + safeTurn,
                status = ShieldStatus.SAFE_WHITELIST,
                pScam = 0.0f,
                threatLevel = ThreatLevel.LOW
            )
            Log.d(tag, "Safe contact speech recorded without scam evaluation")
            return
        }

        val turn = TranscriptTurn(
            speaker = speaker,
            text = text,
            isScamIndicator = current.pScam >= 0.70f
        )
        _session.value = current.copy(
            transcripts = current.transcripts + turn
        )

        // Stream turn to LangGraph backend
        scope.launch {
            val result = repository.submitTurn(
                callId = current.callId,
                callerNumber = current.callerNumber,
                text = text
            )
            result.onSuccess { response ->
                applyTurnResponse(response)
            }
        }
    }

    fun applyTurnResponse(response: TurnResponse) {
        val current = _session.value
        val threatLevel = ThreatLevel.fromProbability(response.pScam)
        
        // Merge extracted identifiers
        val existingValues = current.extractedLeads.map { it.value.lowercase() }.toSet()
        val newLeads = response.extractedIdentifiers.filter { it.value.lowercase() !in existingValues }
        val allLeads = current.extractedLeads + newLeads

        val newStatus = when {
            response.shouldHandover || response.pScam >= 0.88f -> ShieldStatus.DECOY_ACTIVE
            response.pScam >= 0.70f -> ShieldStatus.PENDING_TAKEOVER
            else -> ShieldStatus.MONITORING
        }

        _session.value = current.copy(
            pScam = response.pScam,
            threatLevel = threatLevel,
            status = newStatus,
            signals = (current.signals + response.signals).distinct(),
            extractedLeads = allLeads,
            category = response.category ?: current.category,
            decoyReply = response.decoyReply ?: current.decoyReply,
            handoverAudioUrl = response.handoverAudioUrl ?: current.handoverAudioUrl
        )

        // Check Handover Condition: should_handover == true OR p_scam >= 0.88
        if ((response.shouldHandover || response.pScam >= 0.88f) && !current.isHandoverComplete) {
            executeHandoverProtocol(
                audioUrl = response.handoverAudioUrl,
                decoyReply = response.decoyReply
            )
        }
    }

    fun executeHandoverProtocol(audioUrl: String? = null, decoyReply: String? = null) {
        val current = _session.value
        if (current.isKnownContact || current.isHandoverComplete) return

        Log.w(tag, "🚨 EXECUTING HANDOVER TO RAMESH BOT (p_scam=${current.pScam})")

        // 1. Victim Protection: Haptic waveform vibration
        triggerWarningHaptic()

        // 2. Audio Cut: Strict microphone mute so victim's voice does NOT pass to scammer
        muteMicrophone(true)

        // 3. Mark state as DECOY ACTIVE
        val replyText = decoyReply ?: "Arrey beta, suno toh zara... Mujhe theek se sunai nahi de raha hai... kaun bol rahe hain aap?"
        val handoverTurn = TranscriptTurn(
            speaker = "decoy",
            text = "🤖 [RAMESH SHARMA DECOY ENGAGED]: \"$replyText\" (Victim mic locked — voice not passing to scammer)"
        )
        
        _session.value = current.copy(
            status = ShieldStatus.DECOY_ACTIVE,
            isMicMuted = true,
            isHandoverComplete = true,
            decoyReply = replyText,
            transcripts = current.transcripts + handoverTurn
        )

        // 4. Inject Decoy Voice Stream
        val targetAudio = audioUrl ?: current.handoverAudioUrl
        decoyAudioPlayer.playHandoverVoice(targetAudio)

        // 5. Emit handover event to listeners (UI alert banner / overlay)
        scope.launch {
            _handoverEvents.emit("SCAMMER_INTERCEPTED")
        }

        // 6. Trigger n8n Automation & SMS notification to family contact (+919922996010)
        scope.launch {
            repository.dispatchN8nAlert(
                callId = current.callId,
                callerNumber = current.callerNumber,
                pScam = current.pScam,
                category = current.category ?: "DIGITAL_ARREST"
            )
        }
    }

    private fun triggerWarningHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val pattern = longArrayOf(0, 300, 100, 300, 100, 600)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = longArrayOf(0, 300, 100, 300, 100, 600)
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(tag, "Vibration failed: ${e.message}")
        }
    }

    fun muteMicrophone(mute: Boolean) {
        micMuteWatchdogJob?.cancel()
        micMuteWatchdogJob = null

        try {
            audioManager.isMicrophoneMute = mute
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            Log.d(tag, "Microphone mute set to $mute")
        } catch (e: Exception) {
            Log.e(tag, "Failed to set mic mute: ${e.message}")
        }

        if (mute) {
            // Watchdog ensuring hardware mic stays strictly muted while scammer is being baited
            micMuteWatchdogJob = scope.launch(Dispatchers.IO) {
                while (isActive && _session.value.isHandoverComplete) {
                    try {
                        if (!audioManager.isMicrophoneMute) {
                            audioManager.isMicrophoneMute = true
                            Log.w(tag, "Watchdog re-enforced microphone mute to prevent victim voice leak")
                        }
                    } catch (_: Exception) {}
                    kotlinx.coroutines.delay(350)
                }
            }
        }
    }

    fun updateAmplitudes(newAmps: List<Float>) {
        _waveformAmplitudes.value = newAmps
    }

    fun endCall() {
        micMuteWatchdogJob?.cancel()
        micMuteWatchdogJob = null

        val current = _session.value
        if (current.status == ShieldStatus.IDLE) return

        decoyAudioPlayer.stop()
        muteMicrophone(false)

        val duration = ((System.currentTimeMillis() - current.startTimeMs) / 1000).toInt()
        val record = IncidentRecord(
            callId = current.callId,
            callerNumber = current.callerNumber,
            timestamp = current.startTimeMs,
            durationSeconds = duration.coerceAtLeast(12),
            maxScamProb = current.pScam,
            threatLevel = current.threatLevel,
            category = current.category ?: "DIGITAL_ARREST",
            extractedIdentifiers = current.extractedLeads,
            pdfReportUrl = "${com.sajag.shield.BuildConfig.BACKEND_BASE_URL}reports/${current.callId}/pdf"
        )
        incidentRepository.recordCallConclusion(record)

        _session.value = current.copy(
            status = ShieldStatus.CALL_ENDED,
            isMicMuted = false
        )
        Log.d(tag, "Call ended. Recorded incident for ${record.callId}")
    }

    fun resetToStandby() {
        _session.value = CallSession(status = ShieldStatus.IDLE)
    }
}
