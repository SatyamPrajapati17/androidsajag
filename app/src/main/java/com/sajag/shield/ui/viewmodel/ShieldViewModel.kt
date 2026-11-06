package com.sajag.shield.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajag.shield.data.repository.CallScreeningRepository
import com.sajag.shield.data.repository.IncidentRepository
import com.sajag.shield.domain.model.CallSession
import com.sajag.shield.domain.model.IncidentRecord
import com.sajag.shield.domain.model.ShieldStatus
import com.sajag.shield.service.CallStateManager
import com.sajag.shield.service.DecoyAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ShieldViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callStateManager: CallStateManager,
    private val incidentRepository: IncidentRepository,
    private val repository: CallScreeningRepository,
    private val decoyAudioPlayer: DecoyAudioPlayer
) : ViewModel() {

    val session: StateFlow<CallSession> = callStateManager.session
    val waveformAmplitudes: StateFlow<List<Float>> = callStateManager.waveformAmplitudes
    val incidents: StateFlow<List<IncidentRecord>> = incidentRepository.incidents

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _viewingPdfFile = MutableStateFlow<File?>(null)
    val viewingPdfFile: StateFlow<File?> = _viewingPdfFile.asStateFlow()

    private val _isDownloadingPdf = MutableStateFlow(false)
    val isDownloadingPdf: StateFlow<Boolean> = _isDownloadingPdf.asStateFlow()

    private var simulationJob: Job? = null

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    /**
     * Panic Button: "Force Handover to Ramesh Bot"
     */
    fun forceHandoverToRameshBot() {
        callStateManager.executeHandoverProtocol(
            audioUrl = null,
            decoyReply = "Arrey beta, suno toh zara... Mujhe theek se sunai nahi de raha hai... kaun bol rahe hain aap?"
        )
    }

    /**
     * Simulates an incoming scam call with authentic progressive turns
     */
    fun simulateScamCallScenario(scenario: String = "DIGITAL_ARREST") {
        simulationJob?.cancel()
        callStateManager.startCall(
            callerNumber = "+919876543210",
            isKnown = false,
            contactName = null
        )

        simulationJob = viewModelScope.launch {
            delay(1500)
            callStateManager.addTranscriptTurn(
                speaker = "scammer",
                text = "Hello, this is Inspector Rathore from Crime Branch Mumbai Headquarters."
            )

            delay(3500)
            callStateManager.addTranscriptTurn(
                speaker = "scammer",
                text = "A courier parcel in your Aadhaar name has been intercepted with illegal drugs and 16 forged passports."
            )

            delay(4000)
            callStateManager.addTranscriptTurn(
                speaker = "scammer",
                text = "You are placed under Digital Arrest right now under Section 420. Immediately transfer Rs 25,000 security deposit to cyber verification UPI: police.verify@sbi or a police squad will raid your address."
            )
        }
    }

    fun simulateNormalCall() {
        simulationJob?.cancel()
        callStateManager.startCall(
            callerNumber = "+919922996010",
            isKnown = true,
            contactName = "Mom (Family Whitelist)"
        )

        simulationJob = viewModelScope.launch {
            delay(1200)
            callStateManager.addTranscriptTurn(
                speaker = "caller",
                text = "Beta, did you receive the transfer money in your bank account? Police verification was asking about address."
            )
            delay(2500)
            callStateManager.addTranscriptTurn(
                speaker = "user",
                text = "Haan mummy, sab theek hai, transfer mil gaya hai. Aap chinta mat karo."
            )
        }
    }

    fun sendCustomTurn(text: String) {
        if (text.isNotBlank()) {
            callStateManager.addTranscriptTurn(speaker = "scammer", text = text)
        }
    }

    fun endCall() {
        simulationJob?.cancel()
        simulationJob = null
        callStateManager.endCall()
    }

    fun resetStandby() {
        callStateManager.resetToStandby()
    }

    fun downloadAndOpenPdf(callId: String) {
        viewModelScope.launch {
            _isDownloadingPdf.value = true
            val result = incidentRepository.downloadDossierPdf(callId)
            _isDownloadingPdf.value = false
            result.onSuccess { file ->
                _viewingPdfFile.value = file
            }
        }
    }

    fun closePdfViewer() {
        _viewingPdfFile.value = null
    }

    fun getShareablePdfUri(file: File) = incidentRepository.getShareablePdfUri(file)
}
