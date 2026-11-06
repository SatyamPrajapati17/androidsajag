package com.sajag.shield.service

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.sajag.shield.data.repository.CallScreeningRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SajagInCallService : InCallService() {

    @Inject
    lateinit var callStateManager: CallStateManager

    @Inject
    lateinit var repository: CallScreeningRepository

    private val tag = "SajagInCallService"
    private var activeCall: Call? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(tag, "Call onStateChanged: state=$state")

            when (state) {
                Call.STATE_RINGING -> {
                    Log.d(tag, "Call STATE_RINGING")
                }
                Call.STATE_DIALING -> {
                    Log.d(tag, "Call STATE_DIALING")
                }
                Call.STATE_ACTIVE -> {
                    Log.i(tag, "Call STATE_ACTIVE — Launching Audio Capture & Live HUD Overlay")
                    activeCall = call
                    CallAudioCaptureService.start(applicationContext)
                    OverlayService.start(applicationContext)
                }
                Call.STATE_DISCONNECTED -> {
                    Log.i(tag, "Call STATE_DISCONNECTED — Terminating shield session")
                    CallAudioCaptureService.stop(applicationContext)
                    OverlayService.stop(applicationContext)
                    callStateManager.endCall()
                    activeCall = null
                }
            }
        }

        override fun onConferenceableCallsChanged(call: Call?, conferenceableCalls: MutableList<Call>?) {
            super.onConferenceableCallsChanged(call, conferenceableCalls)
            Log.d(tag, "Conferenceable calls changed: ${conferenceableCalls?.size}")
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(tag, "onCallAdded: details=${call.details}")
        activeCall = call
        call.registerCallback(callCallback)

        val rawNumber = call.details.handle?.schemeSpecificPart ?: "+919876543210"
        if (callStateManager.session.value.status == com.sajag.shield.domain.model.ShieldStatus.IDLE) {
            CoroutineScope(Dispatchers.IO).launch {
                val (isKnown, contactName) = repository.checkContactWhitelist(rawNumber)
                callStateManager.startCall(rawNumber, isKnown, contactName)
            }
        }

        if (call.state == Call.STATE_ACTIVE) {
            CallAudioCaptureService.start(applicationContext)
            OverlayService.start(applicationContext)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(tag, "onCallRemoved")
        call.unregisterCallback(callCallback)
        if (activeCall == call) {
            activeCall = null
            CallAudioCaptureService.stop(applicationContext)
            OverlayService.stop(applicationContext)
            callStateManager.endCall()
        }
    }

    fun conferenceRetellBot(targetBotNumber: String) {
        try {
            activeCall?.let { current ->
                Log.i(tag, "Initiating call conference bridge to Retell Bot ($targetBotNumber)")
                // Telecom conference initiation
                val conferenceables = current.conferenceableCalls
                if (!conferenceables.isNullOrEmpty()) {
                    current.conference(conferenceables[0])
                    Log.i(tag, "Conferenced successfully with ${conferenceables[0]}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Conference failure: ${e.message}", e)
        }
    }
}
