package com.github.mkalmousli.floating_volume

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class CoolSlider(
    context: Context,
) : LinearLayout(context) {

    enum class Orientation {
        Vertical,
        Horizontal
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val max = MutableStateFlow(100)
    val min = MutableStateFlow(0)
    val value = MutableStateFlow(50)
    val isEnabled = MutableStateFlow(true)
    val orientation = MutableStateFlow(Orientation.Horizontal)

    private val progressView by lazy {
        View(context).apply {
            val progressColor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getColor(android.R.color.system_accent1_500)
            } else {
                android.graphics.Color.parseColor("#2196F3")
            }
            
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 100f // Fully rounded ends
                setColor(progressColor)
            }
            background = drawable
        }
    }

    init {
        val trackBgColor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_neutral2_800)
        } else {
            android.graphics.Color.parseColor("#44000000") // Translucent dark
        }
        val trackDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 100f
            setColor(trackBgColor)
        }
        background = trackDrawable
        
        addView(progressView)
        gravity = Gravity.BOTTOM


        setOnTouchListener { _, event ->
            if (!isEnabled.value || event == null) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val size = if (orientation.value == Orientation.Horizontal) width else height
                    val pos = if (orientation.value == Orientation.Horizontal) event.x else event.y
                    val clamped = pos.coerceIn(0f, size.toFloat())

                    val fraction = when (orientation.value) {
                        Orientation.Horizontal -> clamped / size
                        Orientation.Vertical -> 1f - (clamped / size)
                    }

                    val newVal = (min.value + fraction * (max.value - min.value)).roundToInt()
                    value.value = newVal
                    true
                }
                MotionEvent.ACTION_UP -> {
                    performClick()
                    true
                }
                else -> false
            }
        }

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateProgress()
        }

        scope.launch {
            combine(value, min, max) { _, _, _ ->  }.collectLatest {
                updateProgress()
            }
        }
    }

    private fun updateProgress() {
        val range = max.value - min.value
        if (range <= 0 || width == 0 || height == 0) return

        val fraction = ((value.value - min.value).toFloat() / range).coerceIn(0f, 1f)

        if (orientation.value == Orientation.Horizontal) {
            val total = width
            val progressSize = (total * fraction).toInt()
            progressView.updateLayoutParams<LayoutParams> {
                width = progressSize
                height = LayoutParams.MATCH_PARENT
            }
        } else {
            val total = height
            val progressSize = (total * fraction).toInt()
            progressView.updateLayoutParams<LayoutParams> {
                height = progressSize
                width = LayoutParams.MATCH_PARENT
            }
        }
    }

    fun applySettings(settings: com.github.mkalmousli.floating_volume.pigeon_impl.ThemeSettings) {
        // Update Track
        val trackBgColor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_neutral2_800)
        } else {
            android.graphics.Color.parseColor("#44000000") // Translucent dark
        }
        val trackDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = settings.cornerRadius.toFloat()
            setColor(trackBgColor)
        }
        background = trackDrawable

        // Update Progress Bar
        val progressColor = if (settings.enableDynamicColors && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_500)
        } else {
            android.graphics.Color.parseColor("#2196F3")
        }
        val progressDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = settings.cornerRadius.toFloat()
            setColor(progressColor)
        }
        progressView.background = progressDrawable
        progressView.alpha = settings.sliderOpacity.toFloat()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
