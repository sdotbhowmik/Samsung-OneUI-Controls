package com.virtualkeys.oneui.ui

import android.content.Intent
import android.content.res.Configuration
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.virtualkeys.oneui.MainActivity
import com.virtualkeys.oneui.audio.VolumeRepository
import com.virtualkeys.oneui.settings.SettingsStore

class PanelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        isShowing = true
        com.virtualkeys.oneui.overlay.PanelOverlay.hide()
        applyBarIconMode()

        setContent {
            PanelScreen(
                onDismiss = { finish() },
                onOpenSettings = {
                    startActivity(
                        Intent(this, MainActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        ),
                    )
                    finish()
                },
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val delta = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
                VolumeRepository.step(this, VolumeRepository.activeStream(this), delta)
                PanelInteraction.touch()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        isShowing = false
        super.onDestroy()
    }

    private fun applyBarIconMode() {
        val night = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = when (SettingsStore.state.value.themeMode) {
            1 -> true
            2 -> false
            else -> night
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    companion object {
        @Volatile
        var isShowing: Boolean = false
            private set
    }
}
