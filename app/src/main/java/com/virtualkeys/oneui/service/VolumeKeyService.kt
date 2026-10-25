package com.virtualkeys.oneui.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.virtualkeys.oneui.audio.VolumeRepository
import com.virtualkeys.oneui.overlay.PanelOverlay
import com.virtualkeys.oneui.settings.SettingsStore
import com.virtualkeys.oneui.ui.PanelInteraction

class VolumeKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pressedKey = 0
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        SettingsStore.init(this)
        PanelOverlay.hide()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        SettingsStore.init(this)
        val settings = SettingsStore.state.value
        if (!settings.interceptVolumeKeys) return false
        if (com.virtualkeys.oneui.ui.PanelActivity.isShowing) return false

        val key = event.keyCode
        if (key != KeyEvent.KEYCODE_VOLUME_UP && key != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        val am = getSystemService(AudioManager::class.java)
        if (settings.ignoreDuringCall && am != null &&
            (am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION)
        ) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    pressedKey = key
                    longPressTriggered = false
                    scheduleLongPress(key)
                    adjust(key)
                } else if (!longPressTriggered) {
                    adjust(key)
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                cancelLongPress()
                pressedKey = 0
                longPressTriggered = false
                return true
            }
        }
        return false
    }

    private fun scheduleLongPress(key: Int) {
        cancelLongPress()
        if (key != KeyEvent.KEYCODE_VOLUME_DOWN) return

        val runnable = Runnable {
            if (pressedKey == key) {
                longPressTriggered = true
                val stream = VolumeRepository.activeStream(this)
                VolumeRepository.toggleMute(this, stream)
                PanelOverlay.show(this)
                PanelInteraction.touch()
            }
        }
        longPressRunnable = runnable
        handler.postDelayed(runnable, 800L)
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun adjust(key: Int) {
        val stream = VolumeRepository.activeStream(this)
        val delta = if (key == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
        VolumeRepository.step(this, stream, delta)
        PanelOverlay.show(this)
        PanelInteraction.touch()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        PanelOverlay.hide()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        PanelOverlay.hide()
        super.onDestroy()
    }
}
