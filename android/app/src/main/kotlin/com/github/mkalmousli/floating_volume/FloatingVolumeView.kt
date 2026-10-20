package com.github.mkalmousli.floating_volume

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.updateMargins
import com.github.mkalmousli.floating_volume.bloc.MediaControlBloc
import com.github.mkalmousli.floating_volume.bloc.SystemVolumeBloc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlin.math.min

class FloatingVolumeView(
    context: Context
) : LinearLayout(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var currentMaxVolume: Int = 15 // Default to common music max
    @Volatile
    private var currentStreamType: Int = android.media.AudioManager.STREAM_MUSIC

    private val slider by lazy {
        CoolSlider(context).apply {
            // Slider works in 0-1000 range for high precision
            min.value = 0
            max.value = 1000

            // Adapt slider height to the screen height (e.g. 600px on 1024x600 car head-units)
            // so the drag handle and channel icon are never pushed off-screen.
            val screenHeight = try {
                resources.displayMetrics.heightPixels
            } catch (_: Throwable) {
                600
            }
            val adaptiveHeight = (screenHeight * 0.55f).toInt().coerceIn(220, 800)

            layoutParams = LayoutParams(
                60,
                adaptiveHeight
            )
            orientation.value = CoolSlider.Orientation.Vertical


            scope.inIO {
                // Initial sync: System Volume -> Slider Position
                val initialState = SystemVolumeBloc.state
                    .filterIsInstance<SystemVolumeBloc.State.Initialized>()
                    .first()

                currentMaxVolume = initialState.maxVolume
                currentStreamType = initialState.streamType
                value.value = mapSystemVolumeToSlider(initialState.volume)


                // Listener: System Volume changes -> Update Slider
                scope.inIO {
                    SystemVolumeBloc.state
                        .filterIsInstance<SystemVolumeBloc.State.Initialized>()
                        .collectLatest { state ->
                            currentMaxVolume = state.maxVolume
                            val oldStreamType = currentStreamType
                            currentStreamType = state.streamType

                            inMain {
                                if (oldStreamType != currentStreamType) {
                                    updateMediaControlsVisibility()
                                }

                                // Update Channel Icon
                                val iconRes = when (state.streamType) {
                                    android.media.AudioManager.STREAM_ALARM -> android.R.drawable.ic_lock_idle_alarm
                                    android.media.AudioManager.STREAM_RING -> android.R.drawable.ic_lock_silent_mode_off
                                    android.media.AudioManager.STREAM_VOICE_CALL -> android.R.drawable.stat_sys_phone_call
                                    android.media.AudioManager.STREAM_SYSTEM -> android.R.drawable.ic_dialog_info
                                    android.media.AudioManager.STREAM_NOTIFICATION -> android.R.drawable.ic_dialog_email
                                    android.media.AudioManager.STREAM_ACCESSIBILITY -> android.R.drawable.btn_star
                                    else -> android.R.drawable.ic_media_play // Music
                                }
                                try {
                                    channelIv.setImageResource(iconRes)
                                } catch (e: Exception) {
                                    Log.e("FloatingVolume", "Icon failed: $iconRes", e)
                                    channelIv.setImageResource(android.R.drawable.ic_menu_help)
                                }
                            }

                            val newSliderVal = mapSystemVolumeToSlider(state.volume)
                            value.value = newSliderVal
                        }
                }

                // Listener: User dragged the slider -> Update System Volume
                onUserProgressChanged = { sliderVal ->
                    scope.inIO {
                        val targetVol = mapSliderToSystemVolume(sliderVal)

                        // Emit solely on physical human interaction to sever echo loop
                        val currentState = SystemVolumeBloc.state.value as? SystemVolumeBloc.State.Initialized
                        if (currentState == null || currentState.volume != targetVol) {
                            SystemVolumeBloc.event.emit(
                                SystemVolumeBloc.Event.SetVolume(targetVol)
                            )
                        }
                    }
                }

                // Listener: Media State changes
                scope.inIO {
                    MediaControlBloc.mediaState.collectLatest { state ->
                        inMain {
                            updateMediaControlsVisibility()
                            if (state.isPlaying) {
                                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                            } else {
                                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateMediaControlsVisibility() {
        val state = MediaControlBloc.mediaState.value
        if (state.isActive || currentStreamType == android.media.AudioManager.STREAM_MUSIC) {
            mediaControlsContainer.visibility = VISIBLE
        } else {
            mediaControlsContainer.visibility = GONE
        }
    }

    // Mapping Points: (Slider%, Volume%)
    private val mappingPoints = listOf(
        0f to 0f,
        30f to 15f,
        50f to 25f,
        75f to 50f,
        90f to 70f,
        95f to 80f,
        100f to 100f
    )

    private fun mapSliderToSystemVolume(sliderValue: Number): Int {
        val sliderPercent = sliderValue.toFloat() / 10.0f // 0-1000 -> 0-100%
        val volumePercent = interpolate(sliderPercent, mappingPoints, isForward = true)

        val maxVol = currentMaxVolume
        return kotlin.math.round((volumePercent / 100f) * maxVol.toFloat()).toInt().coerceIn(0, maxVol)
    }

    private fun mapSystemVolumeToSlider(systemVolume: Int): Int {
        val maxVol = currentMaxVolume
        val volPercent = if (maxVol > 0) (systemVolume.toFloat() / maxVol.toFloat()) * 100f else 0f

        val sliderPercent = interpolate(volPercent, mappingPoints, isForward = false)
        return (sliderPercent * 10).toInt().coerceIn(0, 1000)
    }

    private fun interpolate(input: Float, points: List<Pair<Float, Float>>, isForward: Boolean): Float {
        val xKey = if (isForward) { p: Pair<Float, Float> -> p.first } else { p -> p.second }
        val yKey = if (isForward) { p: Pair<Float, Float> -> p.second } else { p -> p.first }

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val x1 = xKey(p1)
            val x2 = xKey(p2)

            if (input >= x1 && input <= x2) {
                val y1 = yKey(p1)
                val y2 = yKey(p2)

                if (x2 == x1) return y1

                val t = (input - x1) / (x2 - x1)
                return y1 + t * (y2 - y1)
            }
        }

        return if (input < 0) 0f else 100f
    }

    val handleIv by lazy {
        ImageView(context).apply {
            setImageResource(R.drawable.move)
            alpha = 0.5f

            val size = min(50, 50)
            layoutParams = LayoutParams(
                size,
                size
            ).apply {
                updateMargins(
                    top = 10
                )
            }
        }
    }

    val channelIv by lazy {
        ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_play)
            alpha = 0.8f
            setBackgroundResource(getRippleBackground())
            val size = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 42f, resources.displayMetrics).toInt()
            layoutParams = LayoutParams(size, size).apply {
                bottomMargin = 12
            }
            setOnClickListener {
                scope.inIO {
                    SystemVolumeBloc.event.emit(SystemVolumeBloc.Event.CycleStream)
                }
            }
        }
    }

    private fun getRippleBackground(): Int {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return outValue.resourceId
    }

    private val btnPrev by lazy {
        ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            alpha = 0.8f
            setBackgroundResource(getRippleBackground())
            val size = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            layoutParams = LayoutParams(size, size).apply { topMargin = 10; bottomMargin = 10 }
            setOnClickListener { MediaControlBloc.skipToPrevious(context) }
        }
    }

    private val btnPlayPause by lazy {
        ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_play)
            alpha = 0.8f
            setBackgroundResource(getRippleBackground())
            val size = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 42f, resources.displayMetrics).toInt()
            layoutParams = LayoutParams(size, size).apply { topMargin = 10; bottomMargin = 10 }
            setOnClickListener {
                val state = MediaControlBloc.mediaState.value
                if (state.isPlaying) {
                    MediaControlBloc.pause(context)
                } else {
                    MediaControlBloc.play(context)
                }
            }
        }
    }

    private val btnNext by lazy {
        ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_next)
            alpha = 0.8f
            setBackgroundResource(getRippleBackground())
            val size = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
            layoutParams = LayoutParams(size, size).apply { topMargin = 10; bottomMargin = 10 }
            setOnClickListener { MediaControlBloc.skipToNext(context) }
        }
    }

    private val mediaControlsContainer by lazy {
        LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
            }
            visibility = GONE

            addView(btnPrev)
            addView(btnPlayPause)
            addView(btnNext)
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER

        // V0.0.3 BASIC STYLE: No glassmorphism, no clipToOutline.
        setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val padH = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics
        ).toInt()
        val padV = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
        ).toInt()
        setPadding(padH, padV, padH, padV)

        addView(channelIv)
        addView(slider)
        addView(handleIv)
    }

    fun toggleSliderVisibility() {
        if (slider.visibility == VISIBLE) {
            slider.visibility = GONE
        } else {
            slider.visibility = VISIBLE
        }
    }

    fun showAnimated() {
        visibility = VISIBLE
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
    }

    fun applySettings(settings: com.github.mkalmousli.floating_volume.pigeon_impl.ThemeSettings) {
        slider.applySettings(settings)
    }

    fun hideAnimated() {
        visibility = GONE
        alpha = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
