package com.sajag.shield.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sajag.shield.R
import com.sajag.shield.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class CallAudioCaptureService : Service() {

    @Inject
    lateinit var callStateManager: CallStateManager

    @Inject
    lateinit var speechManager: SpeechTranscriptionManager

    private val tag = "CallAudioCaptureService"
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        startAudioCapture()
        speechManager.startListening()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        if (captureJob != null) return

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            // 250ms buffer: 16000 samples/sec * 2 bytes * 0.25s = 8000 bytes = 4000 shorts
            val chunkSizeInBytes = (sampleRate * 2 * 0.25).toInt()
            val bufferSize = maxOf(minBufferSize, chunkSizeInBytes * 2)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(tag, "AudioRecord VOICE_COMMUNICATION not supported, falling back to MIC")
                    audioRecord?.release()
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                audioRecord?.startRecording()
                Log.d(tag, "AudioRecord started at 16kHz Mono 16-bit PCM (250ms chunks)")

                val audioBuffer = ShortArray(chunkSizeInBytes / 2)

                while (isActive) {
                    val readSamples = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                    if (readSamples > 0) {
                        // If handover active, victim microphone is strictly isolated and zeroed out
                        if (callStateManager.session.value.isHandoverComplete || callStateManager.session.value.isMicMuted) {
                            java.util.Arrays.fill(audioBuffer, 0.toShort())
                        }

                        // Calculate RMS power to drive waveform
                        var sum = 0.0
                        for (i in 0 until readSamples) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / readSamples)
                        val normalized = (rms / 32767.0).toFloat().coerceIn(0.05f, 1.0f)

                        // Send amplitude pulse
                        val amps = List(32) { idx ->
                            val scale = 0.7f + 0.3f * kotlin.math.sin(idx * 0.3f).toFloat()
                            (normalized * scale).coerceIn(0.08f, 1.0f)
                        }
                        callStateManager.updateAmplitudes(amps)
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "AudioRecord capture error: ${e.message}", e)
            } finally {
                safeReleaseAudioRecord()
            }
        }
    }

    private fun safeReleaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(tag, "AudioRecord release exception: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        safeReleaseAudioRecord()
        speechManager.stopListening()
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sajag Scam Shield Active Call Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and intercepts scam calls in real time"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Sajag Mobile Shield Active")
            .setContentText("Monitoring live call for scam & fraud signatures")
            .setSmallIcon(R.drawable.ic_shield_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "sajag_call_shield_channel"
        const val NOTIFICATION_ID = 1930
        const val ACTION_START = "com.sajag.shield.START_CAPTURE"
        const val ACTION_STOP = "com.sajag.shield.STOP_CAPTURE"

        fun start(context: Context) {
            val intent = Intent(context, CallAudioCaptureService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallAudioCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
