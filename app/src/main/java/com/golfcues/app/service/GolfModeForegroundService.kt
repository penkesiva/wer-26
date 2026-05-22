package com.golfcues.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.audio.AudioClassifier
import com.golfcues.app.domain.audio.AudioRecorder
import com.golfcues.app.domain.audio.FakeAudioClassifier
import com.golfcues.app.domain.detection.ShotDetectionEngine
import com.golfcues.app.domain.detection.ShotDetectionResult
import com.golfcues.app.domain.model.AudioStatus
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.domain.model.FeedbackType
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.domain.model.ShotEvent
import com.golfcues.app.domain.motion.MotionSensorManager
import com.golfcues.app.domain.motion.MotionStateDetector
import com.golfcues.app.ml.TfliteAudioClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class GolfModeForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: GolfModeNotificationManager
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var motionDetector: MotionStateDetector
    private lateinit var shotEngine: ShotDetectionEngine
    private var classifier: AudioClassifier? = null
    private var currentSession: GolfSession? = null
    private var timerJob: Job? = null
    private var settingsJob: Job? = null
    private var sessionStartElapsed: Long = 0L
    private val currentSettings = AtomicReference(com.golfcues.app.domain.model.DetectionSettings())

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GolfModeNotificationManager.ACTION_STOP) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = GolfModeNotificationManager(this)
        notificationManager.createChannel()
        shotEngine = ShotDetectionEngine()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                stopReceiver,
                IntentFilter(GolfModeNotificationManager.ACTION_STOP),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, IntentFilter(GolfModeNotificationManager.ACTION_STOP))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!hasMicrophonePermission()) {
            GolfModeSessionState.update {
                it.copy(audioStatus = AudioStatus.MUTED_OR_PERMISSION_MISSING, isActive = false)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        if (currentSession != null) {
            return START_STICKY
        }

        serviceScope.launch {
            startGolfMode()
        }
        return START_STICKY
    }

    private suspend fun startGolfMode() {
        val app = application as GolfCuesApplication
        val settings = app.settingsRepository.getSettings()
        currentSettings.set(settings)
        val session = app.sessionRepository.startSession()
        currentSession = session
        sessionStartElapsed = SystemClock.elapsedRealtime()
        shotEngine.reset()

        val notification = notificationManager.buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                GolfModeNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(GolfModeNotificationManager.NOTIFICATION_ID, notification)
        }

        classifier = createClassifier()
        val motionSensorManager = MotionSensorManager(this)
        motionDetector = MotionStateDetector(serviceScope, motionSensorManager) { result ->
            GolfModeSessionState.update { state ->
                state.copy(motionState = result.state)
            }
        }
        motionDetector.start()

        audioRecorder = AudioRecorder(serviceScope, onWindow = { window ->
            processAudioWindow(window)
        }, onError = { error ->
            GolfModeSessionState.update {
                it.copy(audioStatus = AudioStatus.MUTED_OR_PERMISSION_MISSING)
            }
            stopSelf()
        })

        val started = audioRecorder.start()
        if (!started) {
            stopSelf()
            return
        }

        GolfModeSessionState.update {
            LiveSessionState(
                isActive = true,
                session = session,
                audioStatus = AudioStatus.LISTENING,
                motionState = motionDetector.currentState()
            )
        }

        timerJob = serviceScope.launch {
            while (isActive && currentSession != null) {
                val elapsed = SystemClock.elapsedRealtime() - sessionStartElapsed
                GolfModeSessionState.update { it.copy(elapsedMillis = elapsed) }
                delay(500)
            }
        }

        settingsJob = serviceScope.launch {
            app.settingsRepository.observeSettings().collectLatest { currentSettings.set(it) }
        }
    }

    private fun processAudioWindow(window: ShortArray) {
        val session = currentSession ?: return
        val classifierInstance = classifier ?: return
        val result = classifierInstance.classify(window)
        val motionState = motionDetector.currentState()
        val config = com.golfcues.app.domain.detection.ShotDetectionConfig.fromSettings(currentSettings.get())

        val audioStatus = when {
            result.golfHit >= config.sensitivity.threshold() -> AudioStatus.POSSIBLE_HIT_DETECTED
            result.golfHit >= config.sensitivity.threshold() * 0.7f -> AudioStatus.NOISE_IGNORED
            else -> AudioStatus.LISTENING
        }

        GolfModeSessionState.update {
            it.copy(
                latestConfidence = result.golfHit,
                audioStatus = audioStatus,
                motionState = motionState
            )
        }

        when (val detection = shotEngine.evaluate(result.golfHit, motionState, config)) {
            is ShotDetectionResult.Logged -> {
                serviceScope.launch {
                    logEvent(session, detection.eventType, detection.confidence, motionState, null)
                }
            }
            is ShotDetectionResult.Ignored -> {
                serviceScope.launch {
                    logEvent(
                        session,
                        EventType.IGNORED_AUDIO_HIT,
                        detection.confidence,
                        detection.motionState,
                        detection.reason
                    )
                }
            }
            ShotDetectionResult.NoAction -> Unit
        }
    }

    private suspend fun logEvent(
        session: GolfSession,
        eventType: EventType,
        confidence: Float,
        motionState: com.golfcues.app.domain.model.MotionState,
        ignoreReason: String?
    ) {
        val app = application as GolfCuesApplication
        val event = ShotEvent(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            timestampMillis = System.currentTimeMillis(),
            eventType = eventType,
            audioConfidence = confidence,
            motionState = motionState,
            feedbackType = FeedbackType.UNMARKED,
            audioSnippetPath = null,
            ignoreReason = ignoreReason
        )
        app.eventRepository.insertEvent(event)

        val updatedSession = if (eventType == EventType.PROBABLE_SHOT) {
            session.copy(totalProbableShots = session.totalProbableShots + 1)
        } else {
            session
        }
        currentSession = updatedSession
        app.sessionRepository.updateSession(updatedSession)

        val events = GolfModeSessionState.state.value.events
        val visibleEvents = if (eventType == EventType.IGNORED_AUDIO_HIT) {
            events
        } else {
            listOf(event) + events
        }

        GolfModeSessionState.update {
            it.copy(
                session = updatedSession,
                shotCount = updatedSession.totalProbableShots,
                events = visibleEvents.filter { e ->
                    e.eventType != EventType.IGNORED_AUDIO_HIT ||
                        (e.ignoreReason?.contains("cooldown") != true)
                }.take(50)
            )
        }
    }

    private fun createClassifier(): AudioClassifier {
        return try {
            TfliteAudioClassifier(this).takeIf {
                // Use TFLite if model exists in assets
                assets.list("")?.contains("golf_audio_classifier.tflite") == true
            } ?: FakeAudioClassifier()
        } catch (_: Exception) {
            FakeAudioClassifier()
        }
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        serviceScope.launch {
            currentSession?.let { session ->
                val app = application as GolfCuesApplication
                app.sessionRepository.endSessionDirect(session)
            }
        }
        timerJob?.cancel()
        settingsJob?.cancel()
        if (::audioRecorder.isInitialized) audioRecorder.stop()
        if (::motionDetector.isInitialized) motionDetector.stop()
        classifier?.close()
        classifier = null
        currentSession = null
        GolfModeSessionState.reset()
        try {
            unregisterReceiver(stopReceiver)
        } catch (_: IllegalArgumentException) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.golfcues.app.ACTION_START_GOLF_MODE"
        const val ACTION_STOP = "com.golfcues.app.ACTION_STOP_GOLF_MODE"

        fun start(context: Context) {
            val intent = Intent(context, GolfModeForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, GolfModeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
