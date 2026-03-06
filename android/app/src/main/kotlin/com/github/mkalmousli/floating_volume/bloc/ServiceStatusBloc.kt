package com.github.mkalmousli.floating_volume.bloc

import android.content.Context
import android.content.Intent
import com.github.mkalmousli.floating_volume.FloatingVolumeService
import com.github.mkalmousli.floating_volume.inMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

object ServiceStatusBloc {
    private val _state = MutableStateFlow<State>(
        State.Off
    )

    val state = _state.asStateFlow()


    sealed class State {
        object On : State()
        object Off : State()
    }

    fun updateState(newState: State) {
        _state.value = newState
    }
}
