package com.virtualkeys.oneui.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.virtualkeys.oneui.MainActivity
import com.virtualkeys.oneui.ui.PanelInteraction
import com.virtualkeys.oneui.ui.PanelScreen

class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun create() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/**
 * A Compose window drawn by the accessibility service. It needs no
 * "draw over other apps" permission and can appear above any app.
 */
object PanelOverlay {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    @Volatile
    var isShowing: Boolean = false
        private set

    fun show(context: Context) {
        if (isShowing) {
            PanelInteraction.touch()
            return
        }
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val owner = OverlayLifecycleOwner()
        owner.create()

        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            ViewTreeOwners.attach(this, owner, owner, owner)
            setContent {
                PanelScreen(
                    inOverlay = true,
                    onDismiss = { hide() },
                    onOpenSettings = {
                        openSettings(context)
                        hide()
                    },
                )
            }
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP &&
                    (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
                ) {
                    hide()
                    true
                } else {
                    false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            wm.addView(view, params)
        } catch (t: Throwable) {
            owner.destroy()
            return
        }

        view.post { view.requestFocus() }
        windowManager = wm
        composeView = view
        lifecycleOwner = owner
        isShowing = true
        PanelInteraction.touch()
    }

    fun hide() {
        if (!isShowing) return
        val wm = windowManager
        val view = composeView
        try {
            if (wm != null && view != null) wm.removeView(view)
        } catch (_: Throwable) {
        }
        lifecycleOwner?.destroy()
        composeView = null
        windowManager = null
        lifecycleOwner = null
        isShowing = false
    }

    private fun openSettings(context: Context) {
        try {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
            )
        } catch (_: Throwable) {
        }
    }
}
