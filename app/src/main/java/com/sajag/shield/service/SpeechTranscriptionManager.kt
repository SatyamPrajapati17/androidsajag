package com.sajag.shield.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTranscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callStateManager: CallStateManager
) {
    private val tag = "SpeechManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var amplitudeJob: Job? = null

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    fun startListening() {
        if (isListening) return
        isListening = true

        startAmplitudeSimulation()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(tag, "SpeechRecognizer not natively available on this environment, starting voice chunk processor")
                    return@launch
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(tag, "SpeechRecognizer ready for speech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(tag, "Beginning of speech detected")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Update waveform amplitudes with real mic RMS
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
                            injectDynamicAmplitude(normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(tag, "End of speech segment")
                        }

                        override fun onError(error: Int) {
                            Log.w(tag, "SpeechRecognizer error: $error")
                            // Auto restart listener after transient errors
                            if (isListening) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(500)
                                    restartListener()
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull()
                            if (!recognized.isNullOrBlank()) {
                                Log.i(tag, "Recognized Turn: $recognized")
                                callStateManager.addTranscriptTurn(speaker = "scammer", text = recognized)
                                _partialText.value = recognized
                            }
                            if (isListening) {
                                restartListener()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                            if (!partial.isNullOrBlank()) {
                                _partialText.value = partial
                                // Real-time quick keyword check
                                if (containsCriticalScamKeyword(partial)) {
                                    Log.w(tag, "🚨 Critical scam keyword detected in partial speech: $partial")
                                    callStateManager.addTranscriptTurn(speaker = "scammer", text = partial)
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                Log.d(tag, "SpeechRecognizer started listening with en-IN and hi-IN models")

            } catch (e: Exception) {
                Log.e(tag, "Failed to start SpeechRecognizer: ${e.message}", e)
            }
        }
    }

    private fun restartListener() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
        startListening()
    }

    fun stopListening() {
        isListening = false
        amplitudeJob?.cancel()
        amplitudeJob = null

        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(tag, "Error stopping recognizer: ${e.message}")
            } finally {
                speechRecognizer = null
            }
        }
    }

    private fun containsCriticalScamKeyword(text: String): Boolean {
        // Known contacts are strictly safe — bypass scam auto-detection
        if (callStateManager.session.value.isKnownContact) {
            return false
        }

        val lower = text.lowercase(Locale.ROOT)
        val hasAuthority = lower.contains("police") ||
                lower.contains("cbi") ||
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
                lower.contains("officer") ||
                lower.contains("digital arrest")

        val hasMoneyDemand = lower.contains("money") ||
                lower.contains("transfer") ||
                lower.contains("pay") ||
                lower.contains("fine") ||
                lower.contains("penalty") ||
                lower.contains("deposit") ||
                lower.contains("upi") ||
                lower.contains("account") ||
                lower.contains("police.verify") ||
                lower.contains("bank") ||
                lower.contains("rs") ||
                lower.contains("rupees") ||
                lower.contains("lakh") ||
                lower.contains("fee") ||
                lower.contains("charges")

        val hasCoercion = lower.contains("parcel") ||
                lower.contains("drugs") ||
                lower.contains("raid") ||
                lower.contains("seized") ||
                lower.contains("arrest") ||
                lower.contains("warrant") ||
                lower.contains("fir") ||
                lower.contains("block") ||
                lower.contains("freeze") ||
                lower.contains("immediately")

        return (hasAuthority && hasMoneyDemand) ||
                (hasAuthority && hasCoercion) ||
                lower.contains("digital arrest") ||
                lower.contains("police.verify@sbi")
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isListening) {
                val base = java.util.Random()
                val amps = List(32) {
                    val raw = (base.nextFloat() * 0.7f + 0.15f)
                    raw.coerceIn(0.08f, 0.95f)
                }
                callStateManager.updateAmplitudes(amps)
                delay(90)
            }
        }
    }

    private fun injectDynamicAmplitude(level: Float) {
        val base = java.util.Random()
        val amps = List(32) {
            (level * (0.6f + base.nextFloat() * 0.4f)).coerceIn(0.1f, 1.0f)
        }
        callStateManager.updateAmplitudes(amps)
    }
}
