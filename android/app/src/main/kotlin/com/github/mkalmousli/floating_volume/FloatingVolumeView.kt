package com.github.mkalmousli.floating_volume

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import com.github.mkalmousli.floating_volume.bloc.SystemVolumeBloc
import com.github.mkalmousli.floating_volume.bloc.MediaControlBloc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.min

class FloatingVolumeView(
    context: Context
) : LinearLayout(context) {

    //TODO: Use other lifecycle-aware coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main)

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    }

    @Volatile
    private var currentMaxVolume: Int = 15 // Default to common music max
    @Volatile
    private var currentStreamType: Int = android.media.AudioManager.STREAM_MUSIC

    private val slider by lazy {
        CoolSlider(context).apply {
            // Slider works in 0-1000 range for high precision
            min.value = 0
            max.value = 1000
            
            layoutParams = LayoutParams(
                60, // slightly wider for modern feel
                800 // slightly shorter
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
                             
                             if (oldStreamType != currentStreamType) {
                                 inMain {
                                     updateMediaControlsVisibility()
                                 }
                             }
                             
                             // Only update if difference is significant to avoid loops/jitter
                             val newSliderVal = mapSystemVolumeToSlider(state.volume)
                             // Simple check to avoid feedback loop could be added here if needed
                             value.value = newSliderVal
                        }
                }

                // Listener: Slider changes -> Update System Volume
                scope.inIO {
                    value.collectLatest { sliderVal ->
                        val targetVol = mapSliderToSystemVolume(sliderVal)
                        SystemVolumeBloc.event.emit(
                            SystemVolumeBloc.Event.SetVolume(targetVol)
                        )
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
        // map 0-100 to 0-maxVol
        return (volumePercent / 100f * maxVol).toInt().coerceIn(0, maxVol)
    }

    private fun mapSystemVolumeToSlider(systemVolume: Int): Int {
        val maxVol = currentMaxVolume
        val volPercent = if (maxVol > 0) (systemVolume.toFloat() / maxVol.toFloat()) * 100f else 0f
        
        val sliderPercent = interpolate(volPercent, mappingPoints, isForward = false)
        return (sliderPercent * 10).toInt().coerceIn(0, 1000)
    }

    private fun interpolate(input: Float, points: List<Pair<Float, Float>>, isForward: Boolean): Float {
        // Points are (X=Slider, Y=Volume)
        // if isForward: input is Slider(X), find Y
        // if !isForward: input is Volume(Y), find X

        val xKey = if (isForward) { p: Pair<Float, Float> -> p.first } else { p -> p.second }
        val yKey = if (isForward) { p: Pair<Float, Float> -> p.second } else { p -> p.first }

        // Find segment
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i+1]
            
            val x1 = xKey(p1)
            val x2 = xKey(p2)
            
            if (input >= x1 && input <= x2) {
                // Linear Interpolation
                val y1 = yKey(p1)
                val y2 = yKey(p2)
                
                if (x2 == x1) return y1
                
                val t = (input - x1) / (x2 - x1)
                return y1 + t * (y2 - y1)
            }
        }
        
        // Fallback for out of bounds (should not happen with 0-100 constraints)
        return if (input < 0) 0f else 100f
    }

    val handleIv by lazy {
        ImageView(context).apply {
            setImageResource(R.drawable.move) // Replace with your drawable resource
            alpha = 0.5f

            val size = min(
                50,
                50
            )
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
        layoutTransition = android.animation.LayoutTransition()
        orientation = VERTICAL
        gravity = Gravity.CENTER
        
        val paddingPx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        ).toInt()
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        
        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 100f // Highly rounded pill shape
            setColor(android.graphics.Color.parseColor("#33000000")) // Semi-transparent black for glass effect
        }
        background = bgDrawable
        clipToOutline = true
        
        addView(slider)
        addView(mediaControlsContainer)
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
        if (visibility == VISIBLE && alpha == 1f) return
        visibility = VISIBLE
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).setInterpolator(android.view.animation.OvershootInterpolator()).start()
    }

    fun hideAnimated() {
        animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200).setInterpolator(android.view.animation.AnticipateInterpolator()).withEndAction {
            visibility = GONE
        }.start()
    }
}