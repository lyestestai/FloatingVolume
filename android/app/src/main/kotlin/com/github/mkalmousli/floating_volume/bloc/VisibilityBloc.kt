package com.github.mkalmousli.floating_volume.bloc

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


private const val TAG = "VisibilityBloc"

object VisibilityBloc {
    private val _state =
        MutableStateFlow<State>(
            State.Hidden
        )

    val state = _state.asStateFlow()

    val event =
        MutableSharedFlow<Event>(extraBufferCapacity = 16)


    sealed class State {
        object Shown : State()
        object Hidden : State()
    }

    sealed class Event {
        object Show : Event()
        object Hide : Event()
        object Toggle : Event()
    }

    fun show() {
        _state.value = State.Shown
    }

    fun hide() {
        _state.value = State.Hidden
    }

    fun toggle() {
        _state.value = when (_state.value) {
            State.Hidden -> State.Shown
            State.Shown -> State.Hidden
        }
    }

    suspend fun handleServiceStatusChanges() {
        ServiceStatusBloc.state.collect { serviceState ->
            when (serviceState) {
                is ServiceStatusBloc.State.On -> {
                    Log.d(TAG, "Floating service is ON")
                    _state.value = State.Shown
                }
                else -> {
                    Log.d(TAG, "Floating service is OFF or CRASHED")
                    _state.value = State.Hidden
                }
            }
        }
    }

    suspend fun registerEvents() {
        Log.d(TAG, "Registering event observers")
        event.collect {
            Log.d(TAG, "Received event: $it")
            when (it) {
                Event.Show -> {
                    Log.d(TAG, "Showing mode")
                    _state.value = State.Shown
                }

                Event.Hide -> {
                    Log.d(TAG, "Hiding mode")
                    _state.value = State.Hidden
                }

                Event.Toggle -> {
                    Log.d(TAG, "Toggling mode")
                    toggle()
                }
            }
        }
    }
}
