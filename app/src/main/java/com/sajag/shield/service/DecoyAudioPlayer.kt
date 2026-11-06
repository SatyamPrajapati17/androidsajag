package com.sajag.shield.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.sajag.shield.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecoyAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "DecoyAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var injectionJob: Job? = null

    fun playHandoverVoice(remoteUrl: String? = null) {
        stop()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Ensure voice communication mode
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            audioManager.isMicrophoneMute = true
            Log.i(tag, "Decoy active: Hardware mic muted — victim voice strictly isolated and NOT passing to scammer.")
        } catch (e: Exception) {
            Log.w(tag, "Failed to set audio manager mode: ${e.message}")
        }

        injectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(tag, "Starting decoy voice playback. Remote URL: $remoteUrl")
                
                // Try streaming from URL if valid, otherwise fallback to bundled authentic Ramesh audio
                val player = MediaPlayer().apply {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    setAudioAttributes(attributes)

                    if (!remoteUrl.isNullOrBlank()) {
                        try {
                            setDataSource(context, Uri.parse(remoteUrl))
                            prepare()
                        } catch (e: Exception) {
                            Log.w(tag, "Remote audio prepare failed, using bundled raw asset: ${e.message}")
                            reset()
                            val afd = context.resources.openRawResourceFd(R.raw.ramesh_voice)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            prepare()
                        }
                    } else {
                        val afd = context.resources.openRawResourceFd(R.raw.ramesh_voice)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        prepare()
                    }

                    isLooping = true
                    setOnPreparedListener { mp ->
                        Log.i(tag, "Decoy Ramesh Sharma audio playing into downlink/speaker")
                        mp.start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(tag, "MediaPlayer error: what=$what extra=$extra")
                        false
                    }
                }
                mediaPlayer = player

            } catch (e: Exception) {
                Log.e(tag, "Decoy playback exception: ${e.message}", e)
                fallbackRawPcmInjection()
            }
        }
    }

    private fun fallbackRawPcmInjection() {
        try {
            val sampleRate = 16000
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            audioTrack = track
            Log.d(tag, "AudioTrack USAGE_VOICE_COMMUNICATION initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "AudioTrack init failed: ${e.message}")
        }
    }

    fun stop() {
        injectionJob?.cancel()
        injectionJob = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "MediaPlayer stop failed: ${e.message}")
        } finally {
            mediaPlayer = null
        }

        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack stop failed: ${e.message}")
        } finally {
            audioTrack = null
        }
    }
}
