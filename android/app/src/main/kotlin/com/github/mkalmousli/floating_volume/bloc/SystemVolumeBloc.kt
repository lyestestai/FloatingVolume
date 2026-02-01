package com.github.mkalmousli.floating_volume.bloc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

object SystemVolumeBloc {
    private val _state = MutableStateFlow<State>(
        State.Uninitialized
    )

    val state = _state.asStateFlow()

    val event = MutableSharedFlow<Event>()

    sealed class State {
        object Uninitialized : State()
        data class Initialized(
            val volume: Int,
            val streamType: Int,
            val maxVolume: Int
        ) : State()
    }

    sealed class Event {
        data class SetVolume(val volume: Int) : Event()
        object Mute : Event()
    }

    fun getTargetStream(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // 1. Voice Call (Highest Priority)
        if (am.mode == AudioManager.MODE_IN_CALL || 
            am.mode == AudioManager.MODE_IN_COMMUNICATION) {
            return AudioManager.STREAM_VOICE_CALL
        }

        // 2. Ringing (Phone is ringing)
        if (am.mode == AudioManager.MODE_RINGTONE) {
            return AudioManager.STREAM_RING
        }

        // 3. Alarms
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
             val configs = am.activePlaybackConfigurations
             val hasAlarm = configs.any { 
                 it.audioAttributes.usage == android.media.AudioAttributes.USAGE_ALARM 
             }
             if (hasAlarm) return AudioManager.STREAM_ALARM
        }

        // 4. Active Media (Music, Games, most Navigation apps)
        if (am.isMusicActive) {
            return AudioManager.STREAM_MUSIC
        }

        // 5. Fallback/Idle (Notification/Ring)
        return AudioManager.STREAM_NOTIFICATION
    }

    suspend fun observeSystemVolume(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val flow = callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val targetStream = getTargetStream(context)
                    // We might receive updates for other streams, but we should always update our state 
                    // with the current volume of the *target* stream, or re-evaluate target stream.
                    
                    // Actually, if mode changes (e.g. call starts), we won't get a volume change event 
                    // immediately unless volume changes. We might need to listen to MODE changes too?
                    // For now, let's just refresh on any volume change.
                    
                    val currentVol = am.getStreamVolume(targetStream)
                    val maxVol = am.getStreamMaxVolume(targetStream)
                    
                    trySend(
                        State.Initialized(currentVol, targetStream, maxVol)
                    )
                }
            }

            context.registerReceiver(
                receiver,
                IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            )
            awaitClose { context.unregisterReceiver(receiver) }
        }

        flow.collectLatest {
            _state.emit(it as State.Initialized)
        }
    }

    suspend fun initialize(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val targetStream = getTargetStream(context)
        val currentVolume = audioManager.getStreamVolume(targetStream)
        val maxVolume = audioManager.getStreamMaxVolume(targetStream)

        _state.emit(
            State.Initialized(currentVolume, targetStream, maxVolume)
        )
        Log.d("SystemVolumeBloc", "Initialized with volume: $currentVolume for stream: $targetStream (max: $maxVolume)")
    }

    suspend fun handleEvents(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        suspend fun setVolume(volume: Int) {
            val targetStream = getTargetStream(context)
            val maxVolume = audioManager.getStreamMaxVolume(targetStream)
            // Ensure volume is within bounds
            val safeVolume = volume.coerceIn(0, maxVolume)
            
            audioManager.setStreamVolume(
                targetStream,
                safeVolume,
                AudioManager.FLAG_PLAY_SOUND
            )

            _state.emit(
                State.Initialized(safeVolume, targetStream, maxVolume)
            )
        }

        event.collectLatest {
            when (it) {
                is Event.SetVolume -> setVolume(it.volume)
                Event.Mute -> setVolume(0)
            }
        }
    }
}
