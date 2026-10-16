package com.github.mkalmousli.floating_volume.pigeon_impl

import MediaStateStreamHandler
import PigeonEventSink
import com.github.mkalmousli.floating_volume.bloc.MediaControlBloc
import com.github.mkalmousli.floating_volume.inIO
import com.github.mkalmousli.floating_volume.inMain
import MediaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class MediaStateStreamHandlerImpl(
    private val scope: CoroutineScope
) : MediaStateStreamHandler() {
    private var job: Job? = null

    override fun onListen(p0: Any?, sink: PigeonEventSink<MediaState>) {
        job = scope.inIO {
            MediaControlBloc.mediaState.collectLatest { state ->
                scope.inMain {
                    sink.success(state)
                }
            }
        }
    }

    override fun onCancel(p0: Any?) {
        job?.cancel()
        job = null
    }
}
