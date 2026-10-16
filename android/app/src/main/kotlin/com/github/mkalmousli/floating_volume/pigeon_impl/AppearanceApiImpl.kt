package com.github.mkalmousli.floating_volume.pigeon_impl

import android.content.Context
import android.os.Build
import android.util.Log
import com.github.mkalmousli.floating_volume.FloatingVolumeService

class AppearanceApiImpl : AppearanceApi {
    override fun updateSettings(settings: ThemeSettings) {
        Log.d("AppearanceApiImpl", "Received new settings from Flutter: Blur=${settings.enableBlur}, Colors=${settings.enableDynamicColors}")
        FloatingVolumeService.instance?.applyAppearanceSettings(settings)
    }
}
