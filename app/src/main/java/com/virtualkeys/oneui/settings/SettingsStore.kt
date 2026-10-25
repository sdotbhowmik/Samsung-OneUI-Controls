package com.virtualkeys.oneui.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val interceptVolumeKeys: Boolean = true,
    val showShadeCard: Boolean = true,
    val haptics: Boolean = true,
    val autoDismissSeconds: Int = 5,
    val themeMode: Int = 0,
    val accentIndex: Int = 0,
    val showAllStreams: Boolean = false,
    val ignoreDuringCall: Boolean = true,
)

object SettingsStore {

    private const val PREFS = "oneui_controls"
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _state.value = AppSettings(
            interceptVolumeKeys = p.getBoolean("intercept_keys", true),
            showShadeCard = p.getBoolean("shade_card", true),
            haptics = p.getBoolean("haptics", true),
            autoDismissSeconds = p.getInt("auto_dismiss", 5).coerceIn(2, 15),
            themeMode = p.getInt("theme_mode", 0),
            accentIndex = p.getInt("accent_index", 0),
            showAllStreams = p.getBoolean("show_all_streams", false),
            ignoreDuringCall = p.getBoolean("ignore_during_call", true),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val current = _state.value
        val next = transform(current)
        if (next == current) return
        _state.value = next
        prefs?.edit()?.apply {
            putBoolean("intercept_keys", next.interceptVolumeKeys)
            putBoolean("shade_card", next.showShadeCard)
            putBoolean("haptics", next.haptics)
            putInt("auto_dismiss", next.autoDismissSeconds)
            putInt("theme_mode", next.themeMode)
            putInt("accent_index", next.accentIndex)
            putBoolean("show_all_streams", next.showAllStreams)
            putBoolean("ignore_during_call", next.ignoreDuringCall)
            apply()
        }
    }
}
