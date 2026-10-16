package com.github.mkalmousli.floating_volume.pigeon_impl

import MediaApi
import com.github.mkalmousli.floating_volume.bloc.MediaControlBloc

class MediaApiImpl(private val context: android.content.Context) : MediaApi {
    override fun sendMediaAction(action: String, callback: (Result<Unit>) -> Unit) {
        when (action) {
            "play" -> MediaControlBloc.play(context)
            "pause" -> MediaControlBloc.pause(context)
            "next" -> MediaControlBloc.skipToNext(context)
            "prev" -> MediaControlBloc.skipToPrevious(context)
        }
        callback(Result.success(Unit))
    }
}
