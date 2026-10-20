package com.github.mkalmousli.floating_volume

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.mkalmousli.floating_volume.bloc.PositionBloc
import com.github.mkalmousli.floating_volume.bloc.ServiceStatusBloc
import com.github.mkalmousli.floating_volume.bloc.SystemOrientationBloc
import com.github.mkalmousli.floating_volume.bloc.SystemVolumeBloc
import com.github.mkalmousli.floating_volume.bloc.SystemVolumeInPercentageBloc
import com.github.mkalmousli.floating_volume.bloc.VisibilityBloc
import com.github.mkalmousli.floating_volume.receiver.BootReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FloatingVolumeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var isEngineInitialized = false

    @Volatile
    private var isBootDelayActive = false

    @Volatile
    private var isDraggingOverlay = false

    private var screenWakeReceiver: BroadcastReceiver? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val TAG = "FloatingService"
        const val EXTRA_BOOT_DELAY_MS = "extra_boot_delay_ms"

        @Volatile
        var isStoppingIntentionally: Boolean = false

        @SuppressLint("StaticFieldLeak")
        var instance: FloatingVolumeService? = null
            private set
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val name = "Floating Volume Channel"
            val descriptionText = "Channel for Floating Volume notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Const.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }

    private fun createActionPendingIntent(action: NotificationBroadcastReceiver.Action): PendingIntent {
        val i = Intent(this@FloatingVolumeService, NotificationBroadcastReceiver::class.java).apply {
            putExtra("action", action.ordinal)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            this@FloatingVolumeService,
            100 + action.ordinal,
            i,
            flags
        )
    }

    private fun buildForegroundNotification(isVisible: Boolean, customText: String? = null): Notification {
        val contentText = customText ?: if (isVisible) {
            "Floating Volume is visible"
        } else {
            "Floating Volume is hidden"
        }

        return NotificationCompat.Builder(this@FloatingVolumeService, Const.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Floating Volume")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(createActionPendingIntent(NotificationBroadcastReceiver.Action.LAUNCH_MAIN_ACTIVITY))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setDeleteIntent(createActionPendingIntent(NotificationBroadcastReceiver.Action.STOP))
            .addAction(
                android.R.color.transparent,
                "Stop",
                createActionPendingIntent(NotificationBroadcastReceiver.Action.STOP)
            )
            .addAction(
                android.R.color.transparent,
                if (isVisible) "Hide" else "Show",
                createActionPendingIntent(NotificationBroadcastReceiver.Action.TOGGLE_VISIBILITY)
            )
            .build()
    }

    private fun promoteToForeground(isVisible: Boolean, customText: String? = null) {
        try {
            createNotificationChannel()
            val notification = buildForegroundNotification(isVisible, customText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "Fallback to legacy startForeground", e)
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to promote service to foreground", e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        instance = this
        isStoppingIntentionally = false
        CrashHandler.init(this)
        CrashHandler.logEvent(this, TAG, "Service onCreate() called")

        // Synchronously mark service ON so Flutter UI & VisibilityBloc never see a stale Off state
        ServiceStatusBloc.updateState(ServiceStatusBloc.State.On)
        VisibilityBloc.show()

        // Immediately promote to foreground on creation to satisfy Android 8+ 5-second rule
        promoteToForeground(isVisible = true)

        // Save service state for auto-start on boot
        saveServiceState(running = true)

        // Register dynamic receiver for screen wake (ACC ON after MCU sleep on Chinese Head-Units)
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action ?: "unknown"
                    CrashHandler.logEvent(this@FloatingVolumeService, TAG, "screenWakeReceiver received: $action")
                    if (isEngineInitialized && !isBootDelayActive) {
                        scope.inMain {
                            ensureOverlayAttached()
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            screenWakeReceiver = receiver
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register screenWakeReceiver", e)
        }
    }

    private fun startBlocsAndOverlay() {
        val c = this
        val audioManager = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Ensure ServiceStatusBloc is On BEFORE starting the Off-watcher coroutine
        ServiceStatusBloc.updateState(ServiceStatusBloc.State.On)
        VisibilityBloc.show()

        scope.apply {
            inIO {
                SystemVolumeBloc.initialize(c)
                SystemVolumeBloc.observeSystemVolume(c)
            }
            inIO {
                SystemVolumeBloc.handleEvents(c)
            }

            inIO {
                SystemVolumeInPercentageBloc.initialize(audioManager)
                SystemVolumeInPercentageBloc.listenToVolumeChange(audioManager)
            }

            inIO {
                SystemOrientationBloc.initialize(c)
            }
            inIO {
                SystemOrientationBloc.observeSystemOrientation(c)
            }

            inIO {
                PositionBloc.initialize(c)
            }
            inIO {
                PositionBloc.handleOrientationChanges(c)
            }
            inIO {
                PositionBloc.handleEvents(c)
            }

            inIO {
                VisibilityBloc.registerEvents()
            }
            inIO {
                VisibilityBloc.handleServiceStatusChanges()
            }

            inIO {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        android.service.notification.NotificationListenerService.requestRebind(
                            android.content.ComponentName(
                                c,
                                com.github.mkalmousli.floating_volume.media.FloatingNotificationListener::class.java
                            )
                        )
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to request rebind", e)
                }
            }

            inIO {
                ServiceStatusBloc.state
                    .filterIsInstance<ServiceStatusBloc.State.Off>()
                    .first()

                inMain {
                    stopSelf()
                }
            }

            inIO {
                VisibilityBloc.state.collectLatest { state ->
                    val isVisible = state is VisibilityBloc.State.Shown
                    val notification = buildForegroundNotification(isVisible)

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ActivityCompat.checkSelfPermission(
                            this@FloatingVolumeService,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            NotificationManagerCompat.from(this@FloatingVolumeService)
                                .notify(NOTIFICATION_ID, notification)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Failed to update notification", e)
                        }
                    }

                    inMain {
                        when (state) {
                            VisibilityBloc.State.Hidden -> {
                                layoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
                                floatingMuteView.hideAnimated()
                                if (floatingMuteView.isAttachedToWindow) {
                                    try {
                                        windowManager.updateViewLayout(floatingMuteView, layoutParams)
                                    } catch (_: Throwable) {}
                                }
                            }
                            VisibilityBloc.State.Shown -> {
                                layoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
                                floatingMuteView.showAnimated()
                                if (floatingMuteView.isAttachedToWindow) {
                                    try {
                                        windowManager.updateViewLayout(floatingMuteView, layoutParams)
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                }
            }

            inIO {
                showFloatingMuteView()
                inMain {
                    configureTouchListener()
                }
            }

            inIO {
                PositionBloc.state
                    .filterIsInstance<PositionBloc.State.Initialized>()
                    .map { it.position }
                    .collectLatest { position ->
                        if (!isDraggingOverlay) {
                            inMain {
                                if (layoutParams.x != position.x || layoutParams.y != position.y) {
                                    layoutParams.x = position.x
                                    layoutParams.y = position.y
                                    if (floatingMuteView.isAttachedToWindow) {
                                        try {
                                            windowManager.updateViewLayout(floatingMuteView, layoutParams)
                                        } catch (e: Throwable) {
                                            Log.e(TAG, "Failed to update overlay position", e)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val floatingMuteView by lazy {
        FloatingVolumeView(this)
    }

    // Strictly FLAG_NOT_FOCUSABLE (as in v0.0.3 / v0.9.8):
    // Do NOT use FLAG_LAYOUT_NO_LIMITS or FLAG_LAYOUT_IN_SCREEN, which cause Android WindowManager
    // to expand the overlay's input frame and block touches on MainActivity underneath.
    private var layoutParams =
        LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            },
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

    fun applyAppearanceSettings(settings: com.github.mkalmousli.floating_volume.pigeon_impl.ThemeSettings) {
        Log.d(TAG, "Applying Appearance Settings: $settings")
        if (floatingMuteView.isAttachedToWindow) {
            try {
                windowManager.updateViewLayout(floatingMuteView, layoutParams)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update view layout", e)
            }
        }
        floatingMuteView.applySettings(settings)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun configureTouchListener() {
        var startTime = 0L
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingMuteView.handleIv.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDraggingOverlay = true
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    startTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    val newX = initialX + dx.toInt()
                    val newY = initialY + dy.toInt()

                    if (layoutParams.x != newX || layoutParams.y != newY) {
                        layoutParams.x = newX
                        layoutParams.y = newY

                        if (floatingMuteView.isAttachedToWindow) {
                            try {
                                windowManager.updateViewLayout(floatingMuteView, layoutParams)
                            } catch (e: Throwable) {
                                Log.e(TAG, "Failed to move overlay", e)
                            }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDraggingOverlay = false
                    val finalX = layoutParams.x
                    val finalY = layoutParams.y
                    scope.inIO {
                        PositionBloc.event.emit(
                            PositionBloc.Event.UpdatePosition(
                                PositionBloc.Position(finalX, finalY)
                            )
                        )
                    }

                    val duration = System.currentTimeMillis() - startTime
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (event.action == MotionEvent.ACTION_UP &&
                        duration < 250 &&
                        kotlin.math.abs(dx.toDouble()) < 20.0 &&
                        kotlin.math.abs(dy.toDouble()) < 20.0
                    ) {
                        floatingMuteView.toggleSliderVisibility()
                        if (floatingMuteView.isAttachedToWindow) {
                            try {
                                windowManager.updateViewLayout(floatingMuteView, layoutParams)
                            } catch (e: Throwable) {
                                Log.e(TAG, "Failed to toggle slider layout", e)
                            }
                        }
                    }
                    true
                }

                else -> false
            }
        }
    }

    private suspend fun showFloatingMuteView() {
        Log.d(TAG, "Showing Floating Mute View")

        val position = try {
            PositionBloc.state
                .filterIsInstance<PositionBloc.State.Initialized>()
                .map { it.position }
                .first()
        } catch (e: Throwable) {
            PositionBloc.Position(0, 0)
        }

        withContext(Dispatchers.Main) {
            layoutParams.x = position.x
            layoutParams.y = position.y
            ensureOverlayAttached()
        }
    }

    private fun ensureOverlayAttached() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.w(TAG, "Overlay permission not granted yet; cannot attach FloatingVolumeView.")
                CrashHandler.logEvent(this, TAG, "WARNING: Overlay permission (canDrawOverlays) is FALSE! Cannot attach overlay.")
                return
            }
            if (!floatingMuteView.isAttachedToWindow) {
                layoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.addView(floatingMuteView, layoutParams)
                Log.i(TAG, "FloatingVolumeView attached to WindowManager successfully.")
                CrashHandler.logEvent(this, TAG, "FloatingVolumeView attached to WindowManager at (${layoutParams.x}, ${layoutParams.y})")
            }
            if (VisibilityBloc.state.value is VisibilityBloc.State.Shown) {
                layoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
                floatingMuteView.showAnimated()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to attach FloatingVolumeView to WindowManager", e)
            CrashHandler.logEvent(this, TAG, "ERROR attaching overlay: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requestedDelayMs = intent?.getLongExtra(EXTRA_BOOT_DELAY_MS, 0L) ?: 0L
        Log.d(TAG, "onStartCommand called (requestedDelayMs=${requestedDelayMs}ms, isEngineInitialized=$isEngineInitialized)")
        CrashHandler.logEvent(this, TAG, "onStartCommand (requestedDelayMs=${requestedDelayMs}ms, isEngineInitialized=$isEngineInitialized)")

        ServiceStatusBloc.updateState(ServiceStatusBloc.State.On)

        // Every startForegroundService() call on Android 8+ MUST be matched by startForeground()
        if (!isEngineInitialized && requestedDelayMs > 0L) {
            promoteToForeground(
                isVisible = false,
                customText = "Démarrage automatique dans ${requestedDelayMs / 1000}s..."
            )
        } else {
            val isVisible = VisibilityBloc.state.value is VisibilityBloc.State.Shown
            promoteToForeground(isVisible = isVisible)
        }

        if (!isEngineInitialized) {
            isEngineInitialized = true
            scope.inIO {
                if (requestedDelayMs > 0L) {
                    isBootDelayActive = true
                    Log.i(TAG, "Auto-start delay active: waiting ${requestedDelayMs}ms before attaching overlay...")
                    CrashHandler.logEvent(this@FloatingVolumeService, TAG, "Waiting ${requestedDelayMs}ms (${requestedDelayMs / 1000}s) before attaching overlay...")
                    delay(requestedDelayMs)
                    isBootDelayActive = false
                    CrashHandler.logEvent(this@FloatingVolumeService, TAG, "Auto-start delay finished. Initializing BLoCs & overlay.")
                }
                startBlocsAndOverlay()
            }
        } else if (!isBootDelayActive) {
            scope.inMain {
                VisibilityBloc.show()
                ensureOverlayAttached()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        CrashHandler.logEvent(this, TAG, "onTaskRemoved called (isStoppingIntentionally=$isStoppingIntentionally)")
        if (!isStoppingIntentionally) {
            try {
                val prefs = getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
                if (prefs.getBoolean(BootReceiver.KEY_AUTO_START, false)) {
                    Log.i(TAG, "Task removed while service active; scheduling auto-restart in 3s")
                    CrashHandler.logEvent(this, TAG, "Scheduling service auto-restart in 3000ms after task removal")
                    BootReceiver.scheduleServiceStart(applicationContext, 3000L)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to schedule restart on task removed", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service being destroyed (intentional=$isStoppingIntentionally)")
        CrashHandler.logEvent(this, TAG, "Service onDestroy() (intentional=$isStoppingIntentionally)")

        screenWakeReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Throwable) {}
        }
        screenWakeReceiver = null

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}

        if (floatingMuteView.isAttachedToWindow) {
            try {
                windowManager.removeView(floatingMuteView)
            } catch (_: Throwable) {}
        }

        if (isStoppingIntentionally) {
            saveServiceState(running = false)
        } else {
            // Killed by OS / MCU on ACC OFF or low memory: keep last_service_state = true
            // and schedule a safety restart if auto-start is enabled
            try {
                val prefs = getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
                if (prefs.getBoolean(BootReceiver.KEY_AUTO_START, false)) {
                    CrashHandler.logEvent(this, TAG, "Unintentional stop detected; scheduling safety restart in 5000ms")
                    BootReceiver.scheduleServiceStart(applicationContext, 5000L)
                }
            } catch (_: Throwable) {}
        }

        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            audioManager.unregisterMediaButtonEventReceiver(
                android.content.ComponentName(packageName, NotificationBroadcastReceiver::class.java.name)
            )
        } catch (_: Throwable) {}

        ServiceStatusBloc.updateState(ServiceStatusBloc.State.Off)

        scope.cancel()
        instance = null
    }

    /**
     * Save the current service state to SharedPreferences synchronously.
     */
    private fun saveServiceState(running: Boolean) {
        try {
            val prefs = getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(BootReceiver.KEY_LAST_SERVICE_STATE, running).commit()
            Log.d(TAG, "Service state saved: running=$running")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save service state", e)
        }
    }
}
