package com.sajag.shield.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.sajag.shield.data.repository.CallScreeningRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SajagCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var repository: CallScreeningRepository

    @Inject
    lateinit var callStateManager: CallStateManager

    private val tag = "SajagCallScreening"

    override fun onScreenCall(details: Call.Details) {
        val handle = details.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val normalizedNumber = normalizeE164(rawNumber)
        Log.i(tag, "Incoming call screening: raw=$rawNumber normalized=$normalizedNumber")

        CoroutineScope(Dispatchers.IO).launch {
            val (isKnown, contactName) = repository.checkContactWhitelist(normalizedNumber)

            if (isKnown) {
                Log.i(tag, "Contact verified in local whitelist ($contactName). Marking as SAFE call pass-through.")
                callStateManager.startCall(
                    callerNumber = normalizedNumber,
                    isKnown = true,
                    contactName = contactName
                )
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(details, response)
            } else {
                Log.w(tag, "Unknown / unverified caller detected: $normalizedNumber. Initializing Sajag Shield Session!")
                callStateManager.startCall(
                    callerNumber = normalizedNumber,
                    isKnown = false,
                    contactName = null
                )

                // Allow call to ring through to InCallService so on-device interception and transcription can monitor
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .build()
                respondToCall(details, response)
            }
        }
    }

    private fun normalizeE164(number: String): String {
        val digitsOnly = number.replace(Regex("[^0-9+]"), "")
        return when {
            digitsOnly.startsWith("+") -> digitsOnly
            digitsOnly.length == 10 -> "+91$digitsOnly"
            digitsOnly.startsWith("0") && digitsOnly.length == 11 -> "+91" + digitsOnly.substring(1)
            digitsOnly.startsWith("91") && digitsOnly.length == 12 -> "+$digitsOnly"
            else -> if (digitsOnly.isNotEmpty()) "+$digitsOnly" else "+919876543210"
        }
    }
}
