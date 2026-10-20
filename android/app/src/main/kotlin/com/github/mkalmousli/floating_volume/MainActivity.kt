package com.github.mkalmousli.floating_volume

import MediaApi
import MediaStateStreamHandler
import NativeApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.github.mkalmousli.floating_volume.pigeon_impl.FloatingVolumeVisibilityStreamHandlerImpl
import com.github.mkalmousli.floating_volume.pigeon_impl.MediaApiImpl
import com.github.mkalmousli.floating_volume.pigeon_impl.MediaStateStreamHandlerImpl
import com.github.mkalmousli.floating_volume.pigeon_impl.NativeApiImpl
import com.github.mkalmousli.floating_volume.pigeon_impl.ServiceStatusStreamHandlerImpl
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : FlutterActivity() {
    // Dedicated SupervisorJob scope so that no background exception can ever cancel the Pigeon bridge
    private val apiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        try {
            val name = "Floating Volume Channel"
            val descriptionText = "Channel for Floating Volume notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Const.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(this)
        createNotificationChannel()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        CrashHandler.init(this)

        // Register all Pigeon APIs and EventChannels synchronously on the Main Thread
        // BEFORE Dart's main() / initState() executes its first platform channel calls.
        val messenger = flutterEngine.dartExecutor.binaryMessenger

        NativeApi.setUp(
            messenger,
            NativeApiImpl(applicationContext, apiScope)
        )

        ServiceStatusStreamHandler.register(
            messenger,
            ServiceStatusStreamHandlerImpl(apiScope)
        )

        FloatingVolumeVisibilityStreamHandler.register(
            messenger,
            FloatingVolumeVisibilityStreamHandlerImpl(apiScope)
        )

        MediaApi.setUp(
            messenger,
            MediaApiImpl(applicationContext)
        )

        MediaStateStreamHandler.register(
            messenger,
            MediaStateStreamHandlerImpl(apiScope)
        )

        com.github.mkalmousli.floating_volume.pigeon_impl.AppearanceApi.setUp(
            messenger,
            com.github.mkalmousli.floating_volume.pigeon_impl.AppearanceApiImpl()
        )
    }

    override fun onDestroy() {
        apiScope.cancel()
        super.onDestroy()
    }
}
