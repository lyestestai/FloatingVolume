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
        object CycleStream : Event()
    }

    private var manualStream: Int? = null

    // Order of cycling when tapping the icon
    private val cyclableStreams = listOf(
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ACCESSIBILITY
    )

    fun cycleStream(context: Context) {
        val current = try { getTargetStream(context) } catch (e: Exception) { AudioManager.STREAM_MUSIC }
        val idx = cyclableStreams.indexOf(current)
        manualStream = if (idx != -1) {
            cyclableStreams[(idx + 1) % cyclableStreams.size]
        } else {
            AudioManager.STREAM_MUSIC
        }
        
        // Push state update immediately with new target stream
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(manualStream!!)
            val currVol = am.getStreamVolume(manualStream!!)
            _state.tryEmit(State.Initialized(currVol, manualStream!!, maxVol))
        } catch (e: Exception) {
            Log.e("SystemVolumeBloc", "AAOS restricted query for $manualStream", e)
            _state.tryEmit(State.Initialized(0, manualStream!!, 100))
        }
    }

    fun getTargetStream(context: Context): Int {
        if (manualStream != null) return manualStream!!
        
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        try {
            if (am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION) {
                return AudioManager.STREAM_VOICE_CALL
            }
            if (am.mode == AudioManager.MODE_RINGTONE) {
                return AudioManager.STREAM_RING
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 val configs = am.activePlaybackConfigurations
                 val hasAlarm = configs.any { 
                     it.audioAttributes.usage == android.media.AudioAttributes.USAGE_ALARM ||
                     it.audioAttributes.usage == 12 // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                 }
                 if (hasAlarm) return AudioManager.STREAM_ALARM
            }
            if (am.isMusicActive) {
                return AudioManager.STREAM_MUSIC
            }
        } catch (e: Exception) {
            Log.w("SystemVolumeBloc", "AAOS restricted getTargetStream", e)
        }
        return AudioManager.STREAM_MUSIC
    }

    suspend fun observeSystemVolume(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val flow = callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    try {
                        val targetStream = getTargetStream(context)
                        val currentVol = am.getStreamVolume(targetStream)
                        val maxVol = am.getStreamMaxVolume(targetStream)
                        
                        trySend(
                            State.Initialized(currentVol, targetStream, maxVol)
                        )
                    } catch (e: Exception) {
                        Log.e("SystemVolumeBloc", "Broadcast handler crash avoided", e)
                    }
                }
            }

            androidx.core.content.ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
            awaitClose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {}
            }
        }

        flow.collectLatest {
            _state.emit(it as State.Initialized)
        }
    }

    suspend fun initialize(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            val targetStream = getTargetStream(context)
            val currentVolume = audioManager.getStreamVolume(targetStream)
            val maxVolume = audioManager.getStreamMaxVolume(targetStream)
    
            _state.emit(
                State.Initialized(currentVolume, targetStream, maxVolume)
            )
            Log.d("SystemVolumeBloc", "Initialized with volume: $currentVolume for stream: $targetStream (max: $maxVolume)")
        } catch (e: Exception) {
            Log.e("SystemVolumeBloc", "AAOS init fail", e)
        }
    }

    suspend fun handleEvents(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        suspend fun setVolume(volume: Int) {
            try {
                val targetStream = getTargetStream(context)
                val maxVolume = audioManager.getStreamMaxVolume(targetStream)
                val safeVolume = volume.coerceIn(0, maxVolume)
                
                audioManager.setStreamVolume(
                    targetStream,
                    safeVolume,
                    AudioManager.FLAG_PLAY_SOUND
                )
    
                _state.emit(
                    State.Initialized(safeVolume, targetStream, maxVolume)
                )
            } catch (e: Exception) {
                Log.e("SystemVolumeBloc", "AAOS set volume restricted", e)
            }
        }

        event.collectLatest {
            when (it) {
                is Event.SetVolume -> setVolume(it.volume)
                Event.Mute -> setVolume(0)
                Event.CycleStream -> cycleStream(context)
            }
        }
    }
}
