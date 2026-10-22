package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.CommandType
import com.example.data.model.ParsedIntent
import com.example.data.model.PcAction
import com.example.data.model.PcServerConfig
import com.example.data.model.ResolvedContact
import com.example.data.repository.ActionExecutionResult
import com.example.data.repository.AppLauncherRepository
import com.example.data.repository.BiometricAuthHelper
import com.example.data.repository.CalendarRepository
import com.example.data.repository.ContactLookupResult
import com.example.data.repository.ContactRepository
import com.example.data.repository.GeminiIntentParser
import com.example.data.repository.PcBridgeRepository
import com.example.data.repository.PcBridgeResult
import com.example.data.repository.SystemControlRepository
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisVoiceService
import com.example.service.SpeechManager
import com.example.service.SpeechState
import com.example.service.TtsManager
import com.example.ui.components.PermissionStatus
import com.example.ui.components.VisualizerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    CONFIRMATION_PENDING,
    EXECUTING,
    SPEAKING,
    ERROR,
    EMERGENCY_STOP
}

data class JarvisUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val visualizerState: VisualizerState = VisualizerState.IDLE,
    val transcript: String = "",
    val assistantReply: String = "Jarvis ready. Tap mic or say \"Hi Jarvis\".",
    val currentIntent: ParsedIntent? = null,
    val pendingAction: ParsedIntent? = null,
    val countdownRemaining: Int = 5,
    val clarificationContacts: List<ResolvedContact> = emptyList(),
    val clarificationMessage: String? = null,
    val permissionStatus: PermissionStatus = PermissionStatus(false, false, false, false, false),
    val isTtsMuted: Boolean = false,
    val isTorchOn: Boolean = false,
    val isBackgroundServiceRunning: Boolean = false,
    val isWakeWordListening: Boolean = false,
    val pcConnectionStatus: String = "PC Control Idle",
    val executionLog: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as JarvisApplication).database
    private val commandDao = db.commandDao()

    val historyList: StateFlow<List<CommandHistoryEntity>> = commandDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val contactRepo = ContactRepository(application)
    val calendarRepo = CalendarRepository(application)
    val appLauncher = AppLauncherRepository(application)
    val systemControl = SystemControlRepository(application)
    val pcBridge = PcBridgeRepository(application)
    val biometricHelper = BiometricAuthHelper(application)
    val intentParser = GeminiIntentParser()

    val speechManager = SpeechManager(application)
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val pcConfig: StateFlow<PcServerConfig> = pcBridge.config

    private var countdownJob: Job? = null

    init {
        refreshPermissionStatus()
        observeSpeechState()
        observeTorchState()
        observeBackgroundService()
    }

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechManager.speechState.collect { speechState ->
                when (speechState) {
                    is SpeechState.Idle -> {
                        if (_uiState.value.status == AssistantStatus.LISTENING) {
                            _uiState.value = _uiState.value.copy(
                                status = AssistantStatus.IDLE,
                                visualizerState = VisualizerState.IDLE
                            )
                        }
                    }
                    is SpeechState.Ready -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.LISTENING,
                            visualizerState = VisualizerState.LISTENING,
                            assistantReply = "Listening..."
                        )
                    }
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.LISTENING,
                            visualizerState = VisualizerState.LISTENING
                        )
                    }
                    is SpeechState.PartialResult -> {
                        _uiState.value = _uiState.value.copy(
                            transcript = speechState.text
                        )
                    }
                    is SpeechState.FinalResult -> {
                        _uiState.value = _uiState.value.copy(
                            transcript = speechState.text,
                            status = AssistantStatus.PROCESSING,
                            visualizerState = VisualizerState.PROCESSING
                        )
                        processVoiceCommand(speechState.text)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.ERROR,
                            visualizerState = VisualizerState.IDLE,
                            assistantReply = speechState.message
                        )
                    }
                }
            }
        }
    }

    private fun observeTorchState() {
        viewModelScope.launch {
            systemControl.isTorchOn.collect { on ->
                _uiState.value = _uiState.value.copy(isTorchOn = on)
            }
        }
    }

    private fun observeBackgroundService() {
        viewModelScope.launch {
            JarvisVoiceService.isServiceRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isBackgroundServiceRunning = running)
            }
        }
        viewModelScope.launch {
            JarvisVoiceService.isListeningForWakeWord.collect { listening ->
                _uiState.value = _uiState.value.copy(isWakeWordListening = listening)
            }
        }
        viewModelScope.launch {
            JarvisVoiceService.wakeWordDetectedEvent.collect { wakeEvent ->
                if (!wakeEvent.isNullOrEmpty()) {
                    processVoiceCommand(wakeEvent)
                }
            }
        }
    }

    fun toggleBackgroundService() {
        val app = getApplication<Application>()
        if (_uiState.value.isBackgroundServiceRunning) {
            JarvisVoiceService.stop(app)
        } else {
            JarvisVoiceService.start(app)
        }
    }

    fun toggleListening() {
        if (_uiState.value.status == AssistantStatus.LISTENING) {
            speechManager.stopListening()
        } else {
            ttsManager.stop()
            countdownJob?.cancel()
            _uiState.value = _uiState.value.copy(
                pendingAction = null,
                clarificationContacts = emptyList(),
                clarificationMessage = null,
                transcript = "",
                assistantReply = "Listening for your command...",
                status = AssistantStatus.LISTENING,
                visualizerState = VisualizerState.LISTENING
            )
            vibrateFeedback(30)
            speechManager.startListening()
        }
    }

    fun emergencyStop() {
        countdownJob?.cancel()
        countdownJob = null
        speechManager.cancel()
        ttsManager.stop()
        vibrateFeedback(200)

        _uiState.value = _uiState.value.copy(
            status = AssistantStatus.EMERGENCY_STOP,
            visualizerState = VisualizerState.EMERGENCY,
            assistantReply = "EMERGENCY STOPPED. All actions cancelled.",
            pendingAction = null,
            clarificationContacts = emptyList(),
            clarificationMessage = null
        )

        viewModelScope.launch {
            commandDao.insertCommand(
                CommandHistoryEntity(
                    rawCommand = "EMERGENCY STOP",
                    commandType = CommandType.EMERGENCY_STOP.name,
                    targetSummary = "User triggered emergency stop",
                    executionStatus = "CANCELLED",
                    statusMessage = "Immediate abort triggered by user."
                )
            )
            delay(2500)
            if (_uiState.value.status == AssistantStatus.EMERGENCY_STOP) {
                _uiState.value = _uiState.value.copy(
                    status = AssistantStatus.IDLE,
                    visualizerState = VisualizerState.IDLE
                )
            }
        }
    }

    fun processVoiceCommand(commandText: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.PROCESSING,
                visualizerState = VisualizerState.PROCESSING,
                assistantReply = "Processing command..."
            )

            val parsed = intentParser.parseCommand(commandText)

            if (parsed.commandType == CommandType.EMERGENCY_STOP) {
                emergencyStop()
                return@launch
            }

            // Contact Resolution if contact specified
            var resolvedIntent = parsed
            if (!parsed.contactName.isNullOrEmpty()) {
                val lookup = contactRepo.resolveContact(parsed.contactName)
                when (lookup) {
                    is ContactLookupResult.Single -> {
                        resolvedIntent = parsed.copy(
                            resolvedPhoneNumber = lookup.contact.phoneNumber,
                            contactName = lookup.contact.name
                        )
                    }
                    is ContactLookupResult.Multiple -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.IDLE,
                            visualizerState = VisualizerState.IDLE,
                            clarificationContacts = lookup.candidates,
                            clarificationMessage = "Multiple contacts found for \"${parsed.contactName}\". Which one?",
                            assistantReply = "Found ${lookup.candidates.size} contacts matching \"${parsed.contactName}\". Please choose one.",
                            currentIntent = parsed
                        )
                        speakText("Found multiple contacts for ${parsed.contactName}. Which one should I select?")
                        return@launch
                    }
                    is ContactLookupResult.NotFound -> {}
                    is ContactLookupResult.PermissionRequired -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.ERROR,
                            visualizerState = VisualizerState.IDLE,
                            assistantReply = "Contacts permission needed to look up \"${parsed.contactName}\"."
                        )
                        speakText("Please grant Contacts permission so I can locate ${parsed.contactName}.")
                        return@launch
                    }
                }
            }

            _uiState.value = _uiState.value.copy(currentIntent = resolvedIntent)

            // Consequential Action Handling
            if (resolvedIntent.isConsequential) {
                _uiState.value = _uiState.value.copy(
                    status = AssistantStatus.CONFIRMATION_PENDING,
                    visualizerState = VisualizerState.SPEAKING,
                    pendingAction = resolvedIntent,
                    countdownRemaining = 5,
                    assistantReply = resolvedIntent.spokenResponse
                )

                speakText(resolvedIntent.spokenResponse + " Confirming in five seconds.")
                startCountdown(resolvedIntent)
            } else {
                executeAction(resolvedIntent)
            }
        }
    }

    private fun startCountdown(intent: ParsedIntent) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _uiState.value = _uiState.value.copy(countdownRemaining = sec)
                delay(1000)
            }
            confirmPendingAction()
        }
    }

    fun confirmPendingAction() {
        countdownJob?.cancel()
        countdownJob = null
        val action = _uiState.value.pendingAction ?: return
        _uiState.value = _uiState.value.copy(pendingAction = null)
        executeAction(action)
    }

    fun cancelPendingAction() {
        countdownJob?.cancel()
        countdownJob = null
        val action = _uiState.value.pendingAction
        _uiState.value = _uiState.value.copy(
            pendingAction = null,
            status = AssistantStatus.IDLE,
            visualizerState = VisualizerState.IDLE,
            assistantReply = "Action cancelled."
        )
        speakText("Action cancelled.")

        viewModelScope.launch {
            if (action != null) {
                commandDao.insertCommand(
                    CommandHistoryEntity(
                        rawCommand = _uiState.value.transcript,
                        commandType = action.commandType.name,
                        targetSummary = action.contactName ?: action.targetApp ?: "Consequential action",
                        executionStatus = "CANCELLED",
                        statusMessage = "Cancelled by user safeguard."
                    )
                )
            }
        }
    }

    fun selectClarificationContact(contact: ResolvedContact) {
        val current = _uiState.value.currentIntent ?: return
        _uiState.value = _uiState.value.copy(
            clarificationContacts = emptyList(),
            clarificationMessage = null
        )
        val updated = current.copy(
            contactName = contact.name,
            resolvedPhoneNumber = contact.phoneNumber
        )
        if (updated.isConsequential) {
            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.CONFIRMATION_PENDING,
                visualizerState = VisualizerState.SPEAKING,
                pendingAction = updated,
                countdownRemaining = 5,
                assistantReply = "Selected ${contact.name}. Proceeding..."
            )
            speakText("Selected ${contact.name}. Confirming now.")
            startCountdown(updated)
        } else {
            executeAction(updated)
        }
    }

    private fun executeAction(intent: ParsedIntent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.EXECUTING,
                visualizerState = VisualizerState.SPEAKING,
                assistantReply = intent.spokenResponse
            )

            var executionResult = "Executed."
            var executionStatus = "SUCCESS"

            when (intent.commandType) {
                CommandType.PC_COMMAND -> {
                    val pcAct = intent.pcAction ?: PcAction.PING
                    val res = pcBridge.sendPcAction(pcAct, intent.pcPayload)
                    when (res) {
                        is PcBridgeResult.Success -> {
                            executionResult = res.message
                            speakText(res.message)
                        }
                        is PcBridgeResult.Failure -> {
                            executionStatus = "FAILED"
                            executionResult = res.error
                            speakText(res.error)
                        }
                    }
                }

                CommandType.TORCH_TOGGLE -> {
                    val turnOn = when (intent.settingValue) {
                        "on" -> true
                        "off" -> false
                        else -> null
                    }
                    val res = systemControl.toggleTorch(turnOn)
                    when (res) {
                        is ActionExecutionResult.Success -> {
                            executionResult = res.message
                            speakText(res.message)
                        }
                        is ActionExecutionResult.Failure -> {
                            executionStatus = "FAILED"
                            executionResult = res.reason
                            speakText(res.reason)
                        }
                        else -> {}
                    }
                }

                CommandType.VOLUME_CONTROL -> {
                    val res = when (intent.settingValue) {
                        "up" -> systemControl.adjustVolume(true)
                        "down" -> systemControl.adjustVolume(false)
                        "mute" -> systemControl.setMute(true)
                        else -> systemControl.adjustVolume(true)
                    }
                    if (res is ActionExecutionResult.Success) {
                        executionResult = res.message
                        speakText(res.message)
                    }
                }

                CommandType.SYSTEM_SETTING -> {
                    val target = intent.settingTarget ?: "settings"
                    val res = systemControl.openSystemSettings(target)
                    if (res is ActionExecutionResult.Success) {
                        executionResult = res.message
                        speakText(res.message)
                    }
                }

                CommandType.WEB_SEARCH -> {
                    val q = intent.searchQuery ?: "News"
                    val res = systemControl.performWebSearch(q)
                    if (res is ActionExecutionResult.Success) {
                        executionResult = res.message
                        speakText("Searching the web for $q.")
                    }
                }

                CommandType.OPEN_FILE -> {
                    val res = systemControl.openFileManager()
                    if (res is ActionExecutionResult.Success) {
                        executionResult = res.message
                        speakText(res.message)
                    }
                }

                CommandType.LAUNCH_APP -> {
                    val pkg = intent.targetPackage ?: when (intent.targetApp?.lowercase()) {
                        "whatsapp" -> "com.whatsapp"
                        "instagram" -> "com.instagram.android"
                        "youtube" -> "com.google.android.youtube"
                        else -> null
                    }
                    if (pkg != null) {
                        val res = appLauncher.openApp(pkg, intent.targetApp ?: "App")
                        when (res) {
                            is ActionExecutionResult.Success -> {
                                executionResult = res.message
                                speakText(res.message)
                            }
                            is ActionExecutionResult.AppNotInstalled -> {
                                executionStatus = "FAILED"
                                executionResult = "${res.appName} is not installed on device."
                                speakText(executionResult)
                            }
                            is ActionExecutionResult.Failure -> {
                                executionStatus = "FAILED"
                                executionResult = res.reason
                                speakText(res.reason)
                            }
                        }
                    }
                }

                CommandType.YOUTUBE_SEARCH -> {
                    val query = intent.searchQuery ?: "Android Development"
                    val res = appLauncher.openYouTubeSearch(query)
                    if (res is ActionExecutionResult.Success) {
                        executionResult = res.message
                        speakText("Searching YouTube for $query.")
                    }
                }

                CommandType.CALL_PHONE -> {
                    val phone = intent.resolvedPhoneNumber ?: intent.contactName ?: ""
                    val res = appLauncher.makePhoneCall(phone)
                    when (res) {
                        is ActionExecutionResult.Success -> {
                            executionResult = res.message
                            speakText(res.message)
                        }
                        is ActionExecutionResult.Failure -> {
                            executionStatus = "FAILED"
                            executionResult = res.reason
                            speakText(res.reason)
                        }
                        else -> {}
                    }
                }

                CommandType.WHATSAPP_MESSAGE -> {
                    val phone = intent.resolvedPhoneNumber
                    val msg = intent.messageBody ?: ""
                    if (_uiState.value.permissionStatus.hasAccessibility) {
                        JarvisAccessibilityService.pendingAutoSendWhatsApp = true
                    }
                    val res = appLauncher.openWhatsAppMessage(phone, msg)
                    when (res) {
                        is ActionExecutionResult.Success -> {
                            executionResult = res.message
                            speakText("Opened WhatsApp with message.")
                        }
                        is ActionExecutionResult.AppNotInstalled -> {
                            executionStatus = "FAILED"
                            executionResult = "WhatsApp is not installed."
                            speakText(executionResult)
                        }
                        is ActionExecutionResult.Failure -> {
                            executionStatus = "FAILED"
                            executionResult = res.reason
                            speakText(res.reason)
                        }
                    }
                }

                CommandType.WHATSAPP_VIDEO_CALL, CommandType.WHATSAPP_VOICE_CALL -> {
                    val isVideo = intent.commandType == CommandType.WHATSAPP_VIDEO_CALL
                    val phone = intent.resolvedPhoneNumber ?: ""
                    val res = appLauncher.startWhatsAppCall(phone, isVideo)
                    when (res) {
                        is ActionExecutionResult.Success -> {
                            executionResult = res.message
                            speakText(res.message)
                        }
                        is ActionExecutionResult.AppNotInstalled -> {
                            executionStatus = "FAILED"
                            executionResult = "WhatsApp is not installed."
                            speakText(executionResult)
                        }
                        is ActionExecutionResult.Failure -> {
                            executionStatus = "FAILED"
                            executionResult = res.reason
                            speakText(res.reason)
                        }
                    }
                }

                CommandType.READ_CALENDAR -> {
                    if (calendarRepo.hasCalendarPermission()) {
                        val events = calendarRepo.getUpcomingEvents(5)
                        val speechSummary = calendarRepo.formatEventsForSpeech(events)
                        executionResult = speechSummary
                        _uiState.value = _uiState.value.copy(assistantReply = speechSummary)
                        speakText(speechSummary)
                    } else {
                        executionStatus = "FAILED"
                        executionResult = "Calendar permission is required to read events."
                        speakText(executionResult)
                    }
                }

                CommandType.UNKNOWN, CommandType.AMBIGUOUS_CLARIFICATION -> {
                    speakText(intent.spokenResponse)
                }

                else -> {}
            }

            commandDao.insertCommand(
                CommandHistoryEntity(
                    rawCommand = _uiState.value.transcript.ifEmpty { intent.spokenResponse },
                    commandType = intent.commandType.name,
                    targetSummary = intent.targetApp ?: intent.contactName ?: intent.searchQuery ?: intent.settingTarget ?: "Action",
                    executionStatus = executionStatus,
                    statusMessage = executionResult
                )
            )

            delay(2000)
            if (_uiState.value.status == AssistantStatus.EXECUTING) {
                _uiState.value = _uiState.value.copy(
                    status = AssistantStatus.IDLE,
                    visualizerState = VisualizerState.IDLE,
                    executionLog = executionResult
                )
            }
        }
    }

    fun pingPc() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pcConnectionStatus = "Pinging Windows PC...")
            val res = pcBridge.pingPc()
            when (res) {
                is PcBridgeResult.Success -> {
                    _uiState.value = _uiState.value.copy(pcConnectionStatus = res.message)
                    speakText("Connected to PC.")
                }
                is PcBridgeResult.Failure -> {
                    _uiState.value = _uiState.value.copy(pcConnectionStatus = res.error)
                    speakText("Could not reach PC.")
                }
            }
        }
    }

    fun updatePcConfig(newConfig: PcServerConfig) {
        pcBridge.updateConfig(newConfig)
    }

    private fun speakText(text: String) {
        if (!_uiState.value.isTtsMuted) {
            ttsManager.speak(text)
        }
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isTtsMuted
        if (newMuted) ttsManager.stop()
        _uiState.value = _uiState.value.copy(isTtsMuted = newMuted)
    }

    fun refreshPermissionStatus() {
        val ctx = getApplication<Application>()
        val hasMic = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasCal = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasAccessibility = JarvisAccessibilityService.isAccessibilitySettingsEnabled(ctx)

        _uiState.value = _uiState.value.copy(
            permissionStatus = PermissionStatus(hasMic, hasContacts, hasPhone, hasCal, hasAccessibility)
        )
    }

    private fun vibrateFeedback(durationMs: Long) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            commandDao.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        speechManager.destroyRecognizer()
        ttsManager.shutdown()
    }
}
