package com.github.mkalmousli.floating_volume.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.util.Log

class FloatingNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, FloatingNotificationListener::class.java)
            
            // Register listening to active sessions
            com.github.mkalmousli.floating_volume.bloc.MediaControlBloc.initialize(mediaSessionManager, componentName)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission: BIND_NOTIFICATION_LISTENER_SERVICE", e)
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        com.github.mkalmousli.floating_volume.bloc.MediaControlBloc.clear()
    }

    companion object {
        private const val TAG = "FloatingNotifListener"
    }
}
