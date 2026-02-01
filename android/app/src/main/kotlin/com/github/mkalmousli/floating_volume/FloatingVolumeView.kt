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
            setBackgroundColor(
                0xFFCCCCCC.toInt()
            )

            layoutParams = LayoutParams(
                50,
                900
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
                             currentStreamType = state.streamType
                             
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
            }
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

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
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


}