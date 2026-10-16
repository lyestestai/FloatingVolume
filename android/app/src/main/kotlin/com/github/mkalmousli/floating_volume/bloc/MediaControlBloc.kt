package com.github.mkalmousli.floating_volume.bloc

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaControlBloc {
    private const val TAG = "MediaControlBloc"

    private val _mediaState = MutableStateFlow(
        MediaState(title = null, artist = null, isPlaying = false, isActive = false)
    )
    val mediaState = _mediaState.asStateFlow()

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    
    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState()
        }
    }

    fun initialize(sessionManager: MediaSessionManager, componentName: ComponentName) {
        this.mediaSessionManager = sessionManager
        try {
            sessionManager.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
            val activeSessions = sessionManager.getActiveSessions(componentName)
            updateActiveSession(activeSessions)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to initialize active sessions listener", e)
        }
    }

    fun clear() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (e: Exception) {
            // ignore
        }
        activeController?.unregisterCallback(callback)
        activeController = null
        mediaSessionManager = null
        updateState()
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(callback)
        
        activeController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()

        activeController?.registerCallback(callback)
        updateState()
    }

    private fun updateState() {
        val controller = activeController
        if (controller == null) {
            _mediaState.value = MediaState(
                title = null,
                artist = null,
                isPlaying = false,
                isActive = false
            )
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        _mediaState.value = MediaState(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            isActive = true
        )
    }

    fun play(context: android.content.Context) {
        if (activeController != null) {
            activeController?.transportControls?.play()
        } else {
            sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun pause(context: android.content.Context) {
        if (activeController != null) {
            activeController?.transportControls?.pause()
        } else {
            sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
        }
    }

    fun skipToNext(context: android.content.Context) {
        if (activeController != null) {
            activeController?.transportControls?.skipToNext()
        } else {
            sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipToPrevious(context: android.content.Context) {
        if (activeController != null) {
            activeController?.transportControls?.skipToPrevious()
        } else {
            sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    private fun sendMediaKey(context: android.content.Context, keyCode: Int) {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val event = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        audioManager.dispatchMediaKeyEvent(event)
        val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
}
